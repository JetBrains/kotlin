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

public class ConvertForEachToForLoopIntention : JetSelfTargetingOffsetIndependentIntention<JetSimpleNameExpression>(javaClass(), "Replace with a for loop") {
    override fun isApplicableTo(element: JetSimpleNameExpression): Boolean {
        if (element.getReferencedName() != "forEach") return false

        val data = extractData(element) ?: return false
        if (data.functionLiteral.getValueParameters().size() > 1) return false
        if (data.functionLiteral.getBodyExpression() == null) return false

        return true
    }

    override fun applyTo(element: JetSimpleNameExpression, editor: Editor) {
        val (expressionToReplace, receiver, functionLiteral) = extractData(element)!!
        val loop = generateLoop(functionLiteral, receiver)
        expressionToReplace.replace(loop)
    }

    private data class Data(
            val expressionToReplace: JetExpression,
            val receiver: JetExpression,
            val functionLiteral: JetFunctionLiteralExpression
    )

    private fun extractData(nameExpr: JetSimpleNameExpression): Data? {
        val parent = nameExpr.getParent()
        val expression = (when (parent) {
            is JetCallExpression -> parent.getParent() as? JetDotQualifiedExpression
            is JetBinaryExpression -> parent
            else -> null
        } ?: return null) as JetExpression //TODO: submit bug

        val resolvedCall = expression.getResolvedCall(expression.analyze()) ?: return null
        if (DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() != "kotlin.forEach") return null

        val receiver = resolvedCall.getCall().getExplicitReceiver() as? ExpressionReceiver ?: return null
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return null
        val functionLiteral = argument.getArgumentExpression() as? JetFunctionLiteralExpression ?: return null
        return Data(expression, receiver.getExpression(), functionLiteral)
    }

    private fun generateLoop(functionLiteral: JetFunctionLiteralExpression, receiver: JetExpression): JetExpression {
        val factory = JetPsiFactory(functionLiteral)
        val loopRange = JetPsiUtil.safeDeparenthesize(receiver)
        val body = functionLiteral.getBodyExpression()!!
        val parameter = functionLiteral.getValueParameters().singleOrNull()
        return factory.createExpressionByPattern("for($0 in $1){ $2 }", parameter ?: "it", loopRange, body)
    }
}
