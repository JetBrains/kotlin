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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReplaceContainsIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java, "Replace 'contains' call with 'in' operator"
), HighPriorityAction {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        if (element.calleeName != OperatorNameConventions.CONTAINS.asString()) return null

        val resolvedCall = element.toResolvedCall(BodyResolveMode.PARTIAL) ?: return null
        if (!resolvedCall.isReallySuccess()) return null
        val argument = resolvedCall.call.valueArguments.singleOrNull() ?: return null
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.index != 0) return null

        val target = resolvedCall.resultingDescriptor
        val returnType = target.returnType ?: return null
        if (!target.builtIns.isBooleanOrSubtype(returnType)) return null

        if (!element.isReceiverExpressionWithValue()) return null

        val functionDescriptor = getFunctionDescriptor(element) ?: return null

        if (!functionDescriptor.isOperatorOrCompatible) return null

        return element.callExpression!!.calleeExpression!!.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val argument = element.callExpression!!.valueArguments.single().getArgumentExpression()!!
        val receiver = element.receiverExpression

        val psiFactory = KtPsiFactory(element)

        val prefixExpression = element.parent as? KtPrefixExpression
        val expression = if (prefixExpression != null && prefixExpression.operationToken == KtTokens.EXCL) {
            prefixExpression.replace(psiFactory.createExpressionByPattern("$0 !in $1", argument, receiver))
        } else {
            element.replace(psiFactory.createExpressionByPattern("$0 in $1", argument, receiver))
        }

        // Append semicolon to previous statement if needed
        if (argument is KtLambdaExpression) {
            psiFactory.appendSemicolonBeforeLambdaContainingElement(expression)
        }
    }

    private fun getFunctionDescriptor(element: KtDotQualifiedExpression) : FunctionDescriptor? {
        val resolvedCall = element.resolveToCall() ?: return null
        return resolvedCall.resultingDescriptor as? FunctionDescriptor
    }
}
