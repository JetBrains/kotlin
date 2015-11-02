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
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

public class ConvertForEachToForLoopIntention : SelfTargetingOffsetIndependentIntention<KtSimpleNameExpression>(javaClass(), "Replace with a 'for' loop") {
    override fun isApplicableTo(element: KtSimpleNameExpression): Boolean {
        if (element.getReferencedName() != "forEach") return false

        val data = extractData(element) ?: return false
        if (data.functionLiteral.getValueParameters().size() > 1) return false
        if (data.functionLiteral.getBodyExpression() == null) return false

        return true
    }

    override fun applyTo(element: KtSimpleNameExpression, editor: Editor) {
        val (expressionToReplace, receiver, functionLiteral) = extractData(element)!!

        val commentSaver = CommentSaver(expressionToReplace)

        val loop = generateLoop(functionLiteral, receiver)
        val result = expressionToReplace.replace(loop)

        commentSaver.restore(result)
    }

    private data class Data(
            val expressionToReplace: KtExpression,
            val receiver: KtExpression,
            val functionLiteral: KtFunctionLiteralExpression
    )

    private fun extractData(nameExpr: KtSimpleNameExpression): Data? {
        val parent = nameExpr.getParent()
        val expression = (when (parent) {
            is KtCallExpression -> parent.getParent() as? KtDotQualifiedExpression
            is KtBinaryExpression -> parent
            else -> null
        } ?: return null) as KtExpression //TODO: submit bug

        val resolvedCall = expression.getResolvedCall(expression.analyze()) ?: return null
        if (DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() != "kotlin.forEach") return null

        val receiver = resolvedCall.getCall().getExplicitReceiver() as? ExpressionReceiver ?: return null
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return null
        val functionLiteral = argument.getArgumentExpression() as? KtFunctionLiteralExpression ?: return null
        return Data(expression, receiver.getExpression(), functionLiteral)
    }

    private fun generateLoop(functionLiteral: KtFunctionLiteralExpression, receiver: KtExpression): KtExpression {
        val factory = KtPsiFactory(functionLiteral)
        val loopRange = KtPsiUtil.safeDeparenthesize(receiver)
        val body = functionLiteral.getBodyExpression()!!
        val parameter = functionLiteral.getValueParameters().singleOrNull()
        return factory.createExpressionByPattern("for($0 in $1){ $2 }", parameter ?: "it", loopRange, body)
    }
}
