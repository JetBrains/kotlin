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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

public class ReplaceContainsIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(javaClass(), "Replace 'contains' call with 'in' operator"), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorNameConventions.CONTAINS.asString()) return null

        val resolvedCall = element.toResolvedCall(BodyResolveMode.PARTIAL) ?: return null
        if (!resolvedCall.isReallySuccess()) return null
        val argument = resolvedCall.getCall().getValueArguments().singleOrNull() ?: return null
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.index != 0) return null

        val target = resolvedCall.getResultingDescriptor()
        val returnType = target.getReturnType() ?: return null
        if (!target.builtIns.isBooleanOrSubtype(returnType)) return null

        if (!element.isReceiverExpressionWithValue()) return null

        return element.callExpression!!.getCalleeExpression()!!.getTextRange()
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor) {
        val argument = element.callExpression!!.getValueArguments().single().getArgumentExpression()!!
        val receiver = element.getReceiverExpression()

        val psiFactory = KtPsiFactory(element)

        val prefixExpression = element.getParent() as? KtPrefixExpression
        val expression = if (prefixExpression != null && prefixExpression.getOperationToken() == KtTokens.EXCL) {
            prefixExpression.replace(psiFactory.createExpressionByPattern("$0 !in $1", argument, receiver))
        }
        else {
            element.replace(psiFactory.createExpressionByPattern("$0 in $1", argument, receiver))
        }

        // Append semicolon to previous statement if needed
        if (argument is KtFunctionLiteralExpression) {
            val previousElement = KtPsiUtil.skipSiblingsBackwardByPredicate(expression) {
                it.getNode().getElementType() in KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
            }
            if (previousElement != null && previousElement is KtExpression) {
                previousElement.getParent()!!.addAfter(psiFactory.createSemicolon(), previousElement)
            }
        }
    }
}
