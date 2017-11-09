/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

abstract class ConvertToScopeIntention<TExpression : KtExpression>(
        elementType: Class<TExpression>,
        text: String
) : SelfTargetingIntention<TExpression>(elementType, text) {

    protected val BLACKLIST_RECEIVER_NAME = listOf("this", "it")

    protected abstract fun createScopeExpression(factory: KtPsiFactory, element: TExpression): KtExpression?

    protected abstract fun findCallExpressionFrom(scopeExpression: KtExpression): KtCallExpression?

    protected fun KtDotQualifiedExpression.getReceiverExpressionText(): String? = getLeftMostReceiverExpression().text

    protected fun isApplicableWithGivenReceiverText(expression: KtDotQualifiedExpression, receiverExpressionText: String): Boolean {
        if (receiverExpressionText != expression.getReceiverExpressionText()) return false
        val callExpression = expression.callExpression ?: return false
        if (!callExpression.isApplicable()) return false
        val receiverExpression = expression.receiverExpression
        return receiverExpression !is KtDotQualifiedExpression ||
               isApplicableWithGivenReceiverText(receiverExpression, receiverExpressionText)
    }

    protected fun applyWithGivenReceiverText(expression: TExpression, receiverExpressionText: String) {
        val factory = KtPsiFactory(expression)
        val scopeBlockExpression = createScopeExpression(factory, expression) ?: return
        val callExpression = findCallExpressionFrom(scopeBlockExpression) ?: return
        val blockExpression = callExpression.getFirstLambdaArgumentBody() ?: return
        val parent = expression.parent
        val lastExpressionToMove = findLastExpressionToMove(receiverExpressionText, expression)
        val firstTargetExpression =
                if (expression is KtProperty) expression
                else findFirstExpressionToMove(receiverExpressionText, expression)
        val firstExpressionToMove = if (expression is KtProperty) expression.nextSibling else firstTargetExpression
        blockExpression.moveRangeInto(firstExpressionToMove, lastExpressionToMove)

        parent.addBefore(scopeBlockExpression, firstTargetExpression)
        parent.deleteChildRange(firstTargetExpression, lastExpressionToMove)
    }

    protected fun KtExpression.getDotQualifiedSiblingIfAny(forward: Boolean): KtDotQualifiedExpression? {
        val sibling =
                if (forward) getNextSiblingIgnoringWhitespaceAndComments(false)
                else getPrevSiblingIgnoringWhitespaceAndComments(false)
        return sibling as? KtDotQualifiedExpression
    }

    private fun KtCallExpression.getFirstLambdaArgumentBody() =
            lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression

    private fun KtBlockExpression.moveRangeInto(
            firstElement: PsiElement, lastElement: PsiElement
    ) {
        addRange(firstElement, lastElement)
        children.filterIsInstance(KtDotQualifiedExpression::class.java)
                .forEach { it.deleteFirstReceiver() }
    }

    private fun KtCallExpression.isApplicable() = lambdaArguments.isEmpty() && valueArguments.all { it.text !in BLACKLIST_RECEIVER_NAME }

    private fun findFirstExpressionToMove(receiverExpressionText: String, expression: KtExpression) =
            findBoundaryExpression(receiverExpressionText, expression, forward = false)

    private fun findLastExpressionToMove(receiverExpressionText: String, expression: KtExpression) =
            findBoundaryExpression(receiverExpressionText, expression, forward = true)

    private fun findBoundaryExpression(receiverExpressionText: String, expression: KtExpression, forward: Boolean): KtExpression {
        var targetExpression: KtExpression = expression
        while (true) {
            val dotQualifiedSibling = targetExpression.getDotQualifiedSiblingIfAny(forward)
            if (dotQualifiedSibling == null || !isApplicableWithGivenReceiverText(dotQualifiedSibling, receiverExpressionText)) {
                return targetExpression
            }
            targetExpression = dotQualifiedSibling
        }
    }
}