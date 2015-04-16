/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

public class ConvertForEachToForLoopIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Replace with a for loop") {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        val functionLiteral = extractFunctionLiteral(element) ?: return false
        if (functionLiteral.getValueParameters().size() > 1) return false
        if (functionLiteral.getBodyExpression() == null) return false

        if (caretOffset > functionLiteral.getTextRange().getStartOffset()) return false // not available within function literal body

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        if (DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() != "kotlin.forEach") return false
        return resolvedCall.getCall().getExplicitReceiver() is ExpressionReceiver
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val functionLiteral = extractFunctionLiteral(element)!!
        val receiver = element.getCall(element.analyze())!!.getExplicitReceiver() as ExpressionReceiver
        val receiverExpression = receiver.getExpression()
        val loopText = generateLoopText(functionLiteral, receiverExpression)
        val expressionToReplace = receiverExpression.getParent() // it's correct for both JetDotQualifiedExpression and JetBinaryExpression
        expressionToReplace.replace(JetPsiFactory(element).createExpression(loopText))
    }

    private fun extractFunctionLiteral(element: JetExpression): JetFunctionLiteralExpression? {
        return when (element) {
            is JetCallExpression -> element.getValueArguments().singleOrNull()?.getArgumentExpression()
            is JetBinaryExpression -> element.getRight()
            else -> null
        } as? JetFunctionLiteralExpression
    }

    private fun generateLoopText(functionLiteral: JetFunctionLiteralExpression, receiver: JetExpression): String {
        val loopRangeText = JetPsiUtil.safeDeparenthesize(receiver).getText()
        val bodyText = functionLiteral.getBodyExpression()!!.getText()
        val varText = functionLiteral.getValueParameters().singleOrNull()?.getText() ?: "it"
        return "for ($varText in $loopRangeText) { $bodyText }"
    }
}
