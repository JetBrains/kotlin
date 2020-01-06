/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.branchedTransformations

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

class IfThenToSafeAccessInspection @JvmOverloads constructor(private val inlineWithPrompt: Boolean = true) :
    AbstractApplicabilityBasedInspection<KtIfExpression>(KtIfExpression::class.java) {

    override fun isApplicable(element: KtIfExpression): Boolean = isApplicableTo(element, expressionShouldBeStable = true)

    override fun inspectionHighlightRangeInElement(element: KtIfExpression) = element.fromIfKeywordToRightParenthesisTextRangeInThis()

    override fun inspectionText(element: KtIfExpression) = KotlinBundle.message("foldable.if.then")

    override fun inspectionHighlightType(element: KtIfExpression): ProblemHighlightType =
        if (element.shouldBeTransformed()) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION

    override val defaultFixText get() = KotlinBundle.message("simplify.foldable.if.then")

    override fun fixText(element: KtIfExpression): String = fixTextFor(element)

    override val startFixInWriteAction = false

    override fun applyTo(element: KtIfExpression, project: Project, editor: Editor?) {
        convert(element, editor, inlineWithPrompt)
    }

    companion object {
        fun fixTextFor(element: KtIfExpression): String {
            val ifThenToSelectData = element.buildSelectTransformationData()
            return if (ifThenToSelectData?.baseClauseEvaluatesToReceiver() == true) {
                if (ifThenToSelectData.condition is KtIsExpression) {
                    KotlinBundle.message("replace.if.expression.with.safe.cast.expression")
                } else {
                    KotlinBundle.message("remove.redundant.if.expression")
                }
            } else {
                KotlinBundle.message("replace.if.expression.with.safe.access.expression")
            }
        }

        fun convert(ifExpression: KtIfExpression, editor: Editor?, inlineWithPrompt: Boolean) {
            val ifThenToSelectData = ifExpression.buildSelectTransformationData() ?: return

            val factory = KtPsiFactory(ifExpression)
            val resultExpr = runWriteAction {
                val replacedBaseClause = ifThenToSelectData.replacedBaseClause(factory)
                val newExpr = ifExpression.replaced(replacedBaseClause)
                KtPsiUtil.deparenthesize(newExpr)
            }

            if (editor != null && resultExpr is KtSafeQualifiedExpression) {
                resultExpr.inlineReceiverIfApplicable(editor, inlineWithPrompt)
                resultExpr.renameLetParameter(editor)
            }
        }

        fun isApplicableTo(element: KtIfExpression, expressionShouldBeStable: Boolean): Boolean {
            val ifThenToSelectData = element.buildSelectTransformationData() ?: return false
            if (expressionShouldBeStable &&
                !ifThenToSelectData.receiverExpression.isStableSimpleExpression(ifThenToSelectData.context)
            ) return false

            return ifThenToSelectData.clausesReplaceableBySafeCall()
        }

        internal fun KtSafeQualifiedExpression.renameLetParameter(editor: Editor) {
            val callExpression = selectorExpression as? KtCallExpression ?: return
            if (callExpression.calleeExpression?.text != "let") return
            val parameter = callExpression.lambdaArguments.singleOrNull()?.getLambdaExpression()?.valueParameters?.singleOrNull() ?: return
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            editor.caretModel.moveToOffset(parameter.startOffset)
            KotlinVariableInplaceRenameHandler().doRename(parameter, editor, null)
        }
    }
}

private fun IfThenToSelectData.clausesReplaceableBySafeCall(): Boolean = when {
    baseClause == null -> false
    negatedClause == null && baseClause.isUsedAsExpression(context) -> false
    negatedClause != null && !negatedClause.isNullExpression() -> false
    context.diagnostics.forElement(condition)
        .any { it.factory == Errors.SENSELESS_COMPARISON || it.factory == Errors.USELESS_IS_CHECK } -> false
    baseClause.evaluatesTo(receiverExpression) -> true
    (baseClause as? KtCallExpression)?.calleeExpression?.evaluatesTo(receiverExpression) == true
            && baseClause.isCallingInvokeFunction(context) -> true
    baseClause.hasFirstReceiverOf(receiverExpression) -> withoutResultInCallChain(baseClause, context)
    baseClause.anyArgumentEvaluatesTo(receiverExpression) -> true
    receiverExpression is KtThisExpression -> getImplicitReceiver()?.let { it.type == receiverExpression.getType(context) } == true
    else -> false
}

private fun withoutResultInCallChain(expression: KtExpression, context: BindingContext): Boolean {
    if (expression !is KtDotQualifiedExpression || expression.receiverExpression !is KtDotQualifiedExpression) return true
    return !hasResultInCallExpression(expression, context)
}

private fun hasResultInCallExpression(expression: KtExpression, context: BindingContext): Boolean =
    if (expression is KtDotQualifiedExpression)
        returnTypeIsResult(expression.callExpression, context) || hasResultInCallExpression(expression.receiverExpression, context)
    else
        false

private fun returnTypeIsResult(call: KtCallExpression?, context: BindingContext) = call
    ?.getType(context)
    ?.constructor
    ?.declarationDescriptor
    ?.importableFqName == RESULT_FQNAME

private val RESULT_FQNAME = FqName("kotlin.Result")