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

class ConvertToApplyIntention : ConvertToScopeIntention("Convert to apply") {
    override fun findCallExpressionFrom(scopeExpression: KtExpression): KtCallExpression? {
        if (scopeExpression !is KtProperty) return null
        return (scopeExpression.initializer as? KtQualifiedExpression)?.callExpression
    }

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        return when (element) {
            is KtProperty -> element.isApplicable()
            is KtDotQualifiedExpression -> {
                val receiverExpressionText = element.getLeftMostReceiverExpression().text
                isApplicable(element, receiverExpressionText) && element.findTargetProperty(receiverExpressionText)?.isApplicable() ?: false
            }
            else -> false
        }
    }

    private fun KtProperty.isApplicable(): Boolean {
        if (!isLocal) return false
        val nextSibling = getNextSiblingIgnoringWhitespaceAndComments(false) as? KtDotQualifiedExpression
        val localVariableName = name ?: return false
        return nextSibling != null && isApplicable(nextSibling, localVariableName)
    }

    private fun KtDotQualifiedExpression.findTargetProperty(receiverExpressionText: String): KtProperty? {
        val target = getPrevSiblingIgnoringWhitespaceAndComments(false)
        when (target) {
            is KtProperty -> if (target.name == receiverExpressionText) return target
            is KtDotQualifiedExpression -> if (isApplicable(target, receiverExpressionText)) return target.findTargetProperty(receiverExpressionText)
        }
        return null
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        when (element) {
            is KtProperty -> super.applyTo(element, editor)
            is KtDotQualifiedExpression -> {
                val property = element.findTargetProperty(element.getLeftMostReceiverExpression().text) ?: return
                super.applyTo(property, editor)
            }
        }
    }

    override fun getReceiverExpressionText(element: KtExpression): String? {
        return (element as KtProperty).name
    }

    override fun createScopeExpression(factory: KtPsiFactory, element: KtExpression): KtExpression? {
        if (element !is KtProperty) return null
        val receiverExpressionText = getReceiverExpressionText(element) ?: return null
        return factory.createProperty(receiverExpressionText, element.typeReference?.text, element.isVar, "${element.initializer?.text}.apply{}")
    }

    override fun findTargetFirstElement(receiverExpressionText: String, expression: KtExpression) = expression

    override fun moveToBlockElement(blockExpression: KtBlockExpression, firstElement: PsiElement, lastElement: PsiElement) {
        super.moveToBlockElement(blockExpression, firstElement.nextSibling, lastElement)
    }
}