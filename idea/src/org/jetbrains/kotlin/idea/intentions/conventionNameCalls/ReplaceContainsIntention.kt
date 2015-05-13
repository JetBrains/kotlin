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

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class ReplaceContainsIntention : JetSelfTargetingRangeIntention<JetDotQualifiedExpression>(javaClass(), "Replace 'contains' call with 'in' operator") {
    override fun applicabilityRange(element: JetDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorConventions.CONTAINS.asString()) return null
        val resolvedCall = element.toResolvedCall() ?: return null
        if (!resolvedCall.getStatus().isSuccess()) return null
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return null
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.getIndex() != 0) return null

        val target = resolvedCall.getResultingDescriptor()
        val returnType = target.getReturnType() ?: return null
        if (!target.builtIns.isBooleanOrSubtype(returnType)) return null
        return element.callExpression!!.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        val argument = element.callExpression!!.getValueArguments().single().getArgumentExpression()!!
        val receiver = element.getReceiverExpression()

        val psiFactory = JetPsiFactory(element)

        val prefixExpression = element.getParent() as? JetPrefixExpression
        val expression = if (prefixExpression != null && prefixExpression.getOperationToken() == JetTokens.EXCL) {
            prefixExpression.replace(psiFactory.createExpressionByPattern("$0 !in $1", argument, receiver))
        }
        else {
            element.replace(psiFactory.createExpressionByPattern("$0 in $1", argument, receiver))
        }

        // Append semicolon to previous statement if needed
        if (argument is JetFunctionLiteralExpression) {
            val previousElement = JetPsiUtil.skipSiblingsBackwardByPredicate(expression) {
                it.getNode().getElementType() in JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
            }
            if (previousElement != null && previousElement is JetExpression) {
                previousElement.getParent()!!.addAfter(psiFactory.createSemicolon(), previousElement)
            }
        }
    }
}
