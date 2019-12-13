/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    private fun getFunctionDescriptor(element: KtDotQualifiedExpression): FunctionDescriptor? {
        val resolvedCall = element.resolveToCall() ?: return null
        return resolvedCall.resultingDescriptor as? FunctionDescriptor
    }
}
