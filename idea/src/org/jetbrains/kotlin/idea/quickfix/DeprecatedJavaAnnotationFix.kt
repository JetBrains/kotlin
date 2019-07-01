package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

internal class DeprecatedJavaAnnotationFix(
    element: KtAnnotationEntry,
    private val annotationFqName: FqName
) : KotlinQuickFixAction<KtAnnotationEntry>(element) {
    override fun getFamilyName() = "Replace Annotation"
    override fun getText(): String = "Replace annotation with ${annotationFqName.asString()}"

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(project)

        val arguments = updateAnnotation(psiFactory)
        val replacementAnnotation = psiFactory.createAnnotationEntry("@$annotationFqName")
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

        val replaced = runWriteAction {
            element.replaced(replacementAnnotation)
        }
        OptimizeImportsProcessor(project, file).run()
        runWriteAction {
            ShortenReferences.DEFAULT.process(replaced)
        }
    }

    private fun updateAnnotation(psiFactory: KtPsiFactory): List<KtValueArgument> {
        val bindingContext = element?.analyze() ?: return emptyList()

        val descriptor = bindingContext[BindingContext.ANNOTATION, element]
        val name = descriptor?.fqName ?: return emptyList()

        val argumentOutput = mutableListOf<KtValueArgument>()
        if (name == RETENTION_FQ_NAME) {
            for (arg in descriptor.allValueArguments.values) {
                val typeAndValue = arg.value as? Pair<*, *>
                val classId = typeAndValue?.first as? ClassId
                val value = typeAndValue?.second

                if (classId == RETENTION_POLICY_ID) {
                    val argument = when ((value as? Name)?.asString()) {
                        "SOURCE" -> psiFactory.createArgument("kotlin.annotation.AnnotationRetention.SOURCE")
                        "CLASS" -> psiFactory.createArgument("kotlin.annotation.AnnotationRetention.BINARY")
                        "RUNTIME" -> psiFactory.createArgument("kotlin.annotation.AnnotationRetention.RUNTIME")
                        else -> psiFactory.createArgument("${classId.shortClassName}.$value")
                    }
                    argumentOutput.add(argument)
                }
            }
        }

        return argumentOutput
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        private val RETENTION_FQ_NAME = FqName("java.lang.annotation.Retention")

        private val RETENTION_POLICY_ID = ClassId(FqName("java.lang.annotation"), FqName("RetentionPolicy"), false)

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val castedDiagnostic = ErrorsJvm.DEPRECATED_JAVA_ANNOTATION.cast(diagnostic)

            val updatedAnnotation = castedDiagnostic.a as? FqName ?: return null
            val entry = diagnostic.psiElement as? KtAnnotationEntry ?: return null

            return DeprecatedJavaAnnotationFix(entry, updatedAnnotation)
        }

    }
}