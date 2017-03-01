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

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

abstract class ConvertToScopeIntention(text: String) : SelfTargetingIntention<KtExpression>(KtExpression::class.java, text) {
    private val BLACKLIST_RECEIVER_NAME = listOf("this", "it")

    abstract protected fun createScopeExpression(factory: KtPsiFactory, element: KtExpression): KtExpression?

    abstract protected fun findCallExpressionFrom(scopeExpression: KtExpression): KtCallExpression?

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element !is KtDotQualifiedExpression) return false
        val receiverExpressionText = element.getLeftMostReceiverExpression().text
        if (isBlacklistReceiverName(receiverExpressionText)) return false
        if (!isApplicable(element, receiverExpressionText)) return false
        val nextSibling = element.getNextSiblingIgnoringWhitespaceAndComments(false) as? KtDotQualifiedExpression
        if (nextSibling != null && isApplicable(nextSibling, receiverExpressionText)) return true
        val prevSibling = element.getPrevSiblingIgnoringWhitespaceAndComments(false) as? KtDotQualifiedExpression
        return prevSibling != null && isApplicable(prevSibling, receiverExpressionText)
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val receiverExpressionText = getReceiverExpressionText(element) ?: return
        val factory = KtPsiFactory(element)
        val scopeBlockExpression = createScopeExpression(factory, element) ?: return
        val callExpression = findCallExpressionFrom(scopeBlockExpression) ?: return
        val blockExpression = findBlockExpressionFrom(callExpression) ?: return
        val parent = element.parent
        val firstElement = findTargetFirstElement(receiverExpressionText, element)
        val lastElement = findTargetLastElement(receiverExpressionText, element)
        moveToBlockElement(blockExpression, firstElement, lastElement)

        parent.addBefore(scopeBlockExpression, firstElement)
        parent.deleteChildRange(firstElement, lastElement)
    }

    open protected fun getReceiverExpressionText(element: KtExpression): String? {
        return (element as KtDotQualifiedExpression).getLeftMostReceiverExpression().text
    }

    private fun findBlockExpressionFrom(callExpression: KtCallExpression): KtBlockExpression? {
        val lambdaArguments = callExpression.lambdaArguments
        return lambdaArguments[0].getLambdaExpression().bodyExpression ?: return null
    }

    open protected fun moveToBlockElement(blockExpression: KtBlockExpression, firstElement: PsiElement, lastElement: PsiElement) {
        blockExpression.addRange(firstElement, lastElement)
        blockExpression.children
                .filterIsInstance(KtDotQualifiedExpression::class.java)
                .forEach { it.deleteFirstReceiver() }
    }

    open protected fun findTargetFirstElement(receiverExpressionText: String, expression: KtExpression) = findBoundary(receiverExpressionText, expression, true)
    protected fun findTargetLastElement(receiverExpressionText: String, expression: KtExpression) = findBoundary(receiverExpressionText, expression, false)

    protected fun isBlacklistReceiverName(receiverName: String) = BLACKLIST_RECEIVER_NAME.contains(receiverName)

    protected fun isApplicable(callExpression: KtCallExpression): Boolean {
        when {
            callExpression.lambdaArguments.isNotEmpty() -> return false
            callExpression.valueArguments.firstOrNull { BLACKLIST_RECEIVER_NAME.contains(it.text) } != null -> return false
            else -> return true
        }
    }

    protected fun isApplicable(expression: KtDotQualifiedExpression, receiverExpressionText: String): Boolean {
        if (receiverExpressionText != expression.getLeftMostReceiverExpression().text) return false
        val callExpression = expression.callExpression ?: return false
        if (!isApplicable(callExpression)) return false
        val receiverExpression = expression.receiverExpression
        if (receiverExpression is KtDotQualifiedExpression) return isApplicable(receiverExpression, receiverExpressionText)
        return true
    }

    protected fun findBoundary(receiverExpressionText: String, element: PsiElement, forward: Boolean): PsiElement {
        var targetElement = element
        while (true) {
            val sibling = if (forward) targetElement.getPrevSiblingIgnoringWhitespaceAndComments(false) else targetElement.getNextSiblingIgnoringWhitespaceAndComments(false)
            if (sibling !is KtDotQualifiedExpression || !isApplicable(sibling, receiverExpressionText)) return targetElement
            targetElement = sibling
        }
    }

}