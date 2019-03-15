package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

internal class DeprecatedJavaAnnotationFix(element: KtAnnotationEntry,
                                           private val annotationFqName: FqName,
                                           private val arguments: List<KtValueArgument>) : KotlinQuickFixAction<KtAnnotationEntry>(element) {
    override fun getFamilyName() = "Replace Annotation"
    override fun getText(): String = "Replace annotation with ${annotationFqName.asString()}"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return

        val psiFactory = KtPsiFactory(project)

        val argumentString = if(arguments.isEmpty()) {
            ""
        } else {
            "("+ arguments.joinToString(",") { it.text } + ")"
        }

        element.replace(psiFactory.createAnnotationEntry("@" + annotationFqName.shortName() + argumentString))

        for ((java, kotlin) in JavaAnnotationMapper.javaToKotlinNameMap) {
            if (kotlin == annotationFqName) {
                val oldImport = file.importDirectives.find { it -> it.importedFqName == java } ?: return
                oldImport.delete()
                break
            }
        }

        file.importList?.add(psiFactory.createImportDirective(ImportPath(annotationFqName, false, null)))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val castedDiagnostic = ErrorsJvm.DEPRECATED_JAVA_ANNOTATION.cast(diagnostic)

            val updatedAnnotation = castedDiagnostic.a as? FqName ?: return null

            val entry = diagnostic.psiElement as? KtAnnotationEntry ?: return null

            val arguments = mutableListOf<KtValueArgument>()
            entry.valueArguments.forEach {
                (it as KtValueArgument).children.forEach { child ->
                    arguments.add(child.context as KtValueArgument)
                }
            }

            return DeprecatedJavaAnnotationFix(entry, updatedAnnotation, arguments)
        }

    }
}