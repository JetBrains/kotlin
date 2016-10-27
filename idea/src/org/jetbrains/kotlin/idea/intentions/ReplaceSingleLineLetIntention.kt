/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

class ReplaceSingleLineLetInspection : IntentionBasedInspection<KtCallExpression>(ReplaceSingleLineLetIntention::class) {
    override fun inspectionTarget(element: KtCallExpression) = element.calleeExpression
}

class ReplaceSingleLineLetIntention : SelfTargetingOffsetIndependentIntention<KtCallExpression>(
        KtCallExpression::class.java,
        "Remove redundant '.let' call"
) {
    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val bodyExpression = lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return
        val dotQualifiedExpression = bodyExpression as? KtDotQualifiedExpression ?: return
        val parent = element.parent
        when (parent) {
            is KtQualifiedExpression -> {
                val factory = KtPsiFactory(element.project)
                val receiver = parent.receiverExpression
                parent.replace(dotQualifiedExpression.replaceFirstReceiver(factory, receiver, parent.operationSign == KtTokens.SAFE_ACCESS))
            }
            else -> {
                element.replace(dotQualifiedExpression.deleteFirstReceiver())
            }
        }
    }

    private fun KtDotQualifiedExpression.deleteFirstReceiver(): KtExpression {
        val receiver = receiverExpression
        when (receiver) {
            is KtDotQualifiedExpression -> receiver.deleteFirstReceiver()
            else -> selectorExpression?.let { return this.replace(it) as KtExpression }
        }
        return this
    }

    override fun isApplicableTo(element: KtCallExpression): Boolean {
        if (!isLetMethod(element)) return false
        val lambdaExpression = element.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return false
        val bodyExpression = lambdaExpression.bodyExpression?.children?.singleOrNull() ?: return false
        val dotQualifiedExpression = bodyExpression as? KtDotQualifiedExpression ?: return false
        val parameterName = lambdaExpression.getParameterName() ?: return false
        val receiverExpression = dotQualifiedExpression.getLeftMostReceiverExpression()
        if (receiverExpression.text != parameterName) return false
        dotQualifiedExpression.selectorExpression?.let {
            if (it.anyDescendantOfType<KtLambdaExpression>()) return false
        }
        return !dotQualifiedExpression.receiverUsedAsArgument(parameterName)
    }

    private fun isLetMethod(element: KtCallExpression) =
            element.calleeExpression?.text == "let" && element.isMethodCall("kotlin.let")

    private fun KtLambdaExpression.getParameterName(): String? {
        val parameters = valueParameters
        if (parameters.size > 1) return null
        return if (parameters.size == 1) parameters[0].text else "it"
    }

    private fun KtDotQualifiedExpression.receiverUsedAsArgument(receiverName: String): Boolean {
        (selectorExpression as? KtCallExpression)?.valueArguments?.let {
            if (it.any { it.receiverUsedAsArgument(receiverName) }) return true
        }
        return (receiverExpression as? KtDotQualifiedExpression)?.receiverUsedAsArgument(receiverName) ?: false
    }

    private fun KtValueArgument.receiverUsedAsArgument(receiverName: String) =
            text == receiverName || anyDescendantOfType<KtDotQualifiedExpression> {
                it.getLeftMostReceiverExpression().text == receiverName ||
                it.receiverUsedAsArgument(receiverName)
            }
}