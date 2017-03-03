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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments

class ConvertToApplyIntention : ConvertToScopeIntention<KtExpression>(
        KtExpression::class.java, "Convert to apply"
) {
    override fun findCallExpressionFrom(scopeExpression: KtExpression) =
            ((scopeExpression as? KtProperty)?.initializer as? KtQualifiedExpression)?.callExpression

    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        return when (element) {
            is KtProperty -> element.isApplicable()
            is KtDotQualifiedExpression -> {
                val receiverExpressionText = element.getLeftMostReceiverExpression().text
                isApplicableWithGivenReceiverText(element, receiverExpressionText) &&
                element.findTargetProperty(receiverExpressionText)?.isApplicable() ?: false
            }
            else -> false
        }
    }

    private fun KtProperty.isApplicable(): Boolean {
        if (!isLocal) return false
        val localVariableName = name ?: return false
        val firstDotQualified = getDotQualifiedSiblingIfAny(forward = true)
        if (firstDotQualified != null && isApplicableWithGivenReceiverText(firstDotQualified, localVariableName)) {
            val nextDotQualified = firstDotQualified.getDotQualifiedSiblingIfAny(forward = true)
            return nextDotQualified != null && isApplicableWithGivenReceiverText(nextDotQualified, localVariableName)
        }
        return false
    }

    private fun KtDotQualifiedExpression.findTargetProperty(receiverExpressionText: String): KtProperty? {
        val target = getPrevSiblingIgnoringWhitespaceAndComments(false)
        when (target) {
            is KtProperty ->
                if (target.name == receiverExpressionText) {
                    return target
                }
            is KtDotQualifiedExpression ->
                if (isApplicableWithGivenReceiverText(target, receiverExpressionText)) {
                    return target.findTargetProperty(receiverExpressionText)
                }
        }
        return null
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        when (element) {
            is KtProperty -> applyWithGivenReceiverText(element, element.name ?: return)
            is KtDotQualifiedExpression -> {
                val receiverExpressionText = element.getReceiverExpressionText() ?: return
                val property = element.findTargetProperty(receiverExpressionText) ?: return
                applyWithGivenReceiverText(property, receiverExpressionText)
            }
        }
    }

    override fun createScopeExpression(factory: KtPsiFactory, element: KtExpression): KtProperty? {
        if (element !is KtProperty) return null
        val receiverExpressionText = element.name ?: return null
        return factory.createProperty(receiverExpressionText, element.typeReference?.text, element.isVar,
                                      "${element.initializer?.text}.apply{}")
    }
}