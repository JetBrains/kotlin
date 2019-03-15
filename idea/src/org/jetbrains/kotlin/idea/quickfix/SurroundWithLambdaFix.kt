package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class SurroundWithLambdaFix(
    expression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression), HighPriorityAction {

    override fun getFamilyName() = text
    override fun getText() = "Surround with lambda"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val nameReference = element ?: return

        val newExpression = KtPsiFactory(project).buildExpression {
            appendFixedText("{ ")
            appendExpression(nameReference)
            appendFixedText(" }")
        }
        nameReference.replace(newExpression)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private val LOG = Logger.getInstance(SurroundWithLambdaFix::class.java)

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val diagnosticFactory = diagnostic.factory
            val expectedType: KotlinType
            val expressionType: KotlinType
            when (diagnosticFactory) {
                Errors.TYPE_MISMATCH -> {
                    val diagnosticWithParameters = Errors.TYPE_MISMATCH.cast(diagnostic)
                    expectedType = diagnosticWithParameters.a
                    expressionType = diagnosticWithParameters.b
                }
                Errors.CONSTANT_EXPECTED_TYPE_MISMATCH -> {
                    val context = (diagnostic.psiFile as KtFile).analyzeWithContent()
                    val diagnosticWithParameters = Errors.CONSTANT_EXPECTED_TYPE_MISMATCH.cast(diagnostic)

                    val diagnosticElement = diagnostic.psiElement
                    if (!(diagnosticElement is KtExpression)) {
                        LOG.error("Unexpected element: " + diagnosticElement.text)
                        return null
                    }
                    expectedType = diagnosticWithParameters.b
                    expressionType = context.getType(diagnosticElement) ?: return null
                }
                else -> {
                    LOG.error("Unexpected diagnostic: " + DefaultErrorMessages.render(diagnostic))
                    return null
                }
            }

            if (!expectedType.isFunctionType) return null
            if (expectedType.arguments.size != 1) return null
            val lambdaReturnType = expectedType.arguments[0].type

            if (!expressionType.makeNotNullable().isSubtypeOf(lambdaReturnType) &&
                !(expressionType.isPrimitiveNumberType() && lambdaReturnType.isPrimitiveNumberType())
            ) return null

            val diagnosticElement = diagnostic.psiElement as KtExpression
            return SurroundWithLambdaFix(diagnosticElement)
        }
    }
}