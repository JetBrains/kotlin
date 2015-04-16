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
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

public class ConvertToForEachLoopIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Replace with a for each loop") {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        val data = extractData(element) ?: return false
        if (data.functionLiteral.getValueParameters().size() > 1) return false
        if (data.functionLiteral.getValueParameters().size() > 1 || data.functionLiteral.getBodyExpression() == null) return false

        if (caretOffset > data.functionLiteral.getTextRange().getStartOffset()) return false // not available within function literal body

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() == "kotlin.forEach"
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val data = extractData(element)!!
        val loopText = generateLoopText(data.functionLiteral, data.receiver)
        element.replace(JetPsiFactory(element).createExpression(loopText))
    }

    private data class Data(val functionLiteral: JetFunctionLiteralExpression, val receiver: JetExpression)

    private fun extractData(element: JetExpression): Data? {
        when (element) {
            is JetDotQualifiedExpression -> {
                val selector = element.getSelectorExpression() as? JetCallExpression ?: return null
                val argument = selector.getValueArguments().singleOrNull() ?: return null
                val functionLiteral = argument.getArgumentExpression() as? JetFunctionLiteralExpression ?: return null
                return Data(functionLiteral, element.getReceiverExpression())
            }

            is JetBinaryExpression -> {
                val functionLiteral = element.getRight() as? JetFunctionLiteralExpression ?: return null
                return Data(functionLiteral, element.getLeft() ?: return null)
            }

            else -> return null
        }
    }

    private fun generateLoopText(functionLiteral: JetFunctionLiteralExpression, receiver: JetExpression): String {
        val loopRangeText = JetPsiUtil.safeDeparenthesize(receiver).getText()
        val bodyText = functionLiteral.getBodyExpression()!!.getText()
        val varText = functionLiteral.getValueParameters().singleOrNull()?.getText() ?: "it"
        return "for ($varText in $loopRangeText) { $bodyText }"
    }
}
