package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

internal class DeprecatedJavaAnnotationFix(
    element: KtAnnotationEntry,
    private val annotationFqName: FqName
) : KotlinQuickFixAction<KtAnnotationEntry>(element) {
    override fun getFamilyName() = "Replace Annotation"
    override fun getText(): String = "Replace annotation with ${annotationFqName.asString()}"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return

        val psiFactory = KtPsiFactory(project)

        val arguments = updateAnnotation(psiFactory)

        val replacementAnnotation = psiFactory.createAnnotationEntry("@" + annotationFqName.shortName())

        val valueArgumentList = psiFactory.buildValueArgumentList {
            appendFixedText("(")
            arguments.forEach { argument ->
                appendExpression(argument.getArgumentExpression())
            }
            appendFixedText(")")
        }

        if (arguments.isNotEmpty()) {
            replacementAnnotation.add(valueArgumentList)
        }

        element.replace(replacementAnnotation)

        for ((java, kotlin) in JavaAnnotationMapper.javaToKotlinNameMap) {
            if (kotlin == annotationFqName) {
                val oldImport = file.importDirectives.find { it.importedFqName == java } ?: return
                oldImport.delete()
                break
            }
        }

        file.importList?.add(psiFactory.createImportDirective(ImportPath(annotationFqName, false, null)))
    }

    private fun updateAnnotation(psiFactory: KtPsiFactory): List<KtValueArgument> {
        val bindingContext = element!!.analyze()

        val descriptor: AnnotationDescriptor? = bindingContext[BindingContext.ANNOTATION, element]

        val name = descriptor?.fqName ?: return emptyList()

        val argumentOutput: MutableList<KtValueArgument> = mutableListOf()

        val arguments = descriptor.allValueArguments.values
        if (name == FqName("java.lang.annotation.Retention")) {
            for (arg in arguments) {
                val typeAndValue = (arg.value as Pair<*, *>)
                val type: ClassId = typeAndValue.first as ClassId
                val value = typeAndValue.second

                val retentionMatch = type == ClassId(
                    FqName("java.lang.annotation"),
                    FqName("RetentionPolicy"),
                    false
                )

                // Migrate!

                if (retentionMatch) {
                    if (value == Name.identifier("SOURCE")) {
                        argumentOutput.add(psiFactory.createArgument("AnnotationRetention.$value"))
                    } else {
                        // In this case, we add the original back so it won't compile and the author can fix it.
                        argumentOutput.add(psiFactory.createArgument("${type.shortClassName}.$value"))
                    }
                }

            }
        }

        return argumentOutput
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val castedDiagnostic = ErrorsJvm.DEPRECATED_JAVA_ANNOTATION.cast(diagnostic)

            val updatedAnnotation = castedDiagnostic.a as? FqName ?: return null

            val entry = diagnostic.psiElement as? KtAnnotationEntry ?: return null

            return DeprecatedJavaAnnotationFix(entry, updatedAnnotation)
        }

    }
}