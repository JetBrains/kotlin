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
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.idea.intentions.isReceiverExpressionWithValue
import org.jetbrains.kotlin.idea.intentions.toResolvedCall
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReplaceCallWithComparisonInspection : IntentionBasedInspection<KtDotQualifiedExpression>(
        ReplaceCallWithBinaryOperatorIntention::class,
        { qualifiedExpression ->
            val calleeExpression = qualifiedExpression.callExpression?.calleeExpression as? KtSimpleNameExpression
            val identifier = calleeExpression?.getReferencedNameAsName()
            identifier == OperatorNameConventions.EQUALS || identifier == OperatorNameConventions.COMPARE_TO
        }
)

class ReplaceCallWithBinaryOperatorIntention : SelfTargetingRangeIntention<KtDotQualifiedExpression>(
        KtDotQualifiedExpression::class.java,
        "Replace call with binary operator"
), HighPriorityAction {

    private fun IElementType.inverted(): KtSingleValueToken? = when (this) {
        KtTokens.LT -> KtTokens.GT
        KtTokens.GT -> KtTokens.LT

        KtTokens.GTEQ -> KtTokens.LTEQ
        KtTokens.LTEQ -> KtTokens.GTEQ

        else -> null
    }

    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        val calleeExpression = element.callExpression?.calleeExpression as? KtSimpleNameExpression ?: return null
        val operation = operation(calleeExpression) ?: return null

        val resolvedCall = element.toResolvedCall(BodyResolveMode.PARTIAL) ?: return null
        if (!resolvedCall.isReallySuccess()) return null
        if (resolvedCall.call.typeArgumentList != null) return null
        val argument = resolvedCall.call.valueArguments.singleOrNull() ?: return null
        if ((resolvedCall.getArgumentMapping(argument) as ArgumentMatch).valueParameter.index != 0) return null

        if (!element.isReceiverExpressionWithValue()) return null

        text = "Replace with '${operation.value}' operator"
        return calleeExpression.textRange
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val callExpression = element.callExpression ?: return
        val operation = operation(callExpression.calleeExpression as? KtSimpleNameExpression ?: return) ?: return
        val argument = callExpression.valueArguments.single().getArgumentExpression() ?: return
        val receiver = element.receiverExpression

        val factory = KtPsiFactory(element)
        when (operation) {
            KtTokens.EXCLEQ -> {
                val prefixExpression = element.getWrappingPrefixExpressionIfAny() ?: return
                val newExpression = factory.createExpressionByPattern("$0 != $1", receiver, argument)
                prefixExpression.replace(newExpression)
            }
            in OperatorConventions.COMPARISON_OPERATIONS -> {
                val binaryParent = element.parent as? KtBinaryExpression ?: return
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                binaryParent.replace(newExpression)
            }
            else -> {
                val newExpression = factory.createExpressionByPattern("$0 ${operation.value} $1", receiver, argument)
                element.replace(newExpression)
            }
        }

    }

    private fun PsiElement.getWrappingPrefixExpressionIfAny() =
            (getLastParentOfTypeInRow<KtParenthesizedExpression>() ?: this).parent as? KtPrefixExpression

    private fun operation(calleeExpression: KtSimpleNameExpression): KtSingleValueToken? {
        val identifier = calleeExpression.getReferencedNameAsName()
        val dotQualified = calleeExpression.parent.parent as? KtDotQualifiedExpression ?: return null
        return when (identifier) {
            OperatorNameConventions.EQUALS -> {
                val resolvedCall = dotQualified.toResolvedCall(BodyResolveMode.PARTIAL) ?: return null
                val overriddenDescriptors = resolvedCall.resultingDescriptor.findOriginalTopMostOverriddenDescriptors()
                if (overriddenDescriptors.none { it.fqNameUnsafe.asString() == "kotlin.Any.equals" }) return null

                val prefixExpression = dotQualified.getWrappingPrefixExpressionIfAny()
                if (prefixExpression != null && prefixExpression.operationToken == KtTokens.EXCL) KtTokens.EXCLEQ
                else KtTokens.EQEQ
            }
            OperatorNameConventions.COMPARE_TO -> {
                // callee -> call -> DotQualified -> Binary
                val binaryParent = dotQualified.parent as? KtBinaryExpression ?: return null
                val notZero = when {
                    binaryParent.right?.text == "0" -> binaryParent.left
                    binaryParent.left?.text == "0" -> binaryParent.right
                    else -> return null
                }
                if (notZero != dotQualified) return null
                val token = binaryParent.operationToken as? KtSingleValueToken ?: return null
                if (token in OperatorConventions.COMPARISON_OPERATIONS) {
                    if (notZero == binaryParent.left) token else token.inverted()
                }
                else {
                    null
                }
            }
            else -> OperatorConventions.BINARY_OPERATION_NAMES.inverse()[identifier]
        }
    }
}
