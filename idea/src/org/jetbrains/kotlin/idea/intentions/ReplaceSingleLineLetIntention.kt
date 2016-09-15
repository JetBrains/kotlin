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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class ReplaceSingleLineLetInspection :
        IntentionBasedInspection<KtCallExpression>(ReplaceSingleLineLetIntention::class)

class ReplaceSingleLineLetIntention : SelfTargetingOffsetIndependentIntention<KtCallExpression>(
        KtCallExpression::class.java,
        "Replace single line '.let' call"
) {
    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val lambdaExpression = element.lambdaArguments[0].getLambdaExpression()
        val bodyExpression = lambdaExpression.bodyExpression ?: return
        val dotQualifiedExpression = bodyExpression.children[0] as? KtDotQualifiedExpression ?: return
        val parameterName = getParameterNameFromLambdaExpression(lambdaExpression)
        val expressionText = dotQualifiedExpression.text.replace("$parameterName.", "")
        val newExpression = KtPsiFactory(element.project).createExpression(element.parent.text.replace(element.text, "") + expressionText)
        element.parent.replace(newExpression)
    }

    override fun isApplicableTo(element: KtCallExpression): Boolean {
        if (!isLetMethod(element)) return false
        val lambdaExpression = element.lambdaArguments[0].getLambdaExpression()
        val bodyExpressions = lambdaExpression.bodyExpression?.children ?: return false
        if (bodyExpressions.size > 1) return false
        val dotQualifiedExpression = bodyExpressions[0] as? KtDotQualifiedExpression ?: return false
        val parameterName = getParameterNameFromLambdaExpression(lambdaExpression) ?: return false
        val receiverExpression = getLeftMostReceiverExpression(dotQualifiedExpression)
        if (receiverExpression.text != parameterName) return false
        return !isUseReceiver(dotQualifiedExpression, parameterName)
    }

    private fun isLetMethod(element: KtCallExpression): Boolean {
        if (element.calleeExpression?.text != "let") return false
        return isMethodCall(element, "kotlin.let")
    }

    private fun isMethodCall(expression: KtExpression, fqMethodName: String): Boolean {
        val resolvedCall = expression.getResolvedCall(expression.analyze()) ?: return false
        return resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == fqMethodName
    }

    private fun getParameterNameFromLambdaExpression(lambdaExpression: KtLambdaExpression): String? {
        val parameters = lambdaExpression.valueParameters
        if (parameters.size > 1) return null
        return if (parameters.size == 1) parameters[0].text else "it"
    }

    private fun getLeftMostReceiverExpression(expression: KtDotQualifiedExpression): KtExpression {
        val receiverExpression = expression.receiverExpression
        return if (receiverExpression is KtDotQualifiedExpression) getLeftMostReceiverExpression(receiverExpression) else receiverExpression
    }

    private fun isUseReceiver(expression: KtExpression, receiverName: String): Boolean {
        if (expression !is KtDotQualifiedExpression) return false
        val selectorExpression = expression.selectorExpression
        if ((selectorExpression as? KtCallExpression)?.valueArguments?.singleOrNull { it.text == receiverName } != null) return true
        if (expression.children.size == 0) return false
        return isUseReceiver(expression.receiverExpression, receiverName)
    }
}