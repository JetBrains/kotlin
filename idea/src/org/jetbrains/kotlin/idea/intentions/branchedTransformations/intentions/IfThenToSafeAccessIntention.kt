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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class IfThenToSafeAccessInspection : IntentionBasedInspection<KtIfExpression>(IfThenToSafeAccessIntention::class)

class IfThenToSafeAccessIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(
        KtIfExpression::class.java, "Replace 'if' expression with safe access expression"
) {

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        val condition = element.condition
        val thenClause = element.then
        val elseClause = element.`else`
        return when (condition) {
            is KtBinaryExpression -> {
                val receiverExpression = condition.expressionComparedToNull() ?: return false
                if (!receiverExpression.isStableVariable()) return false
                when (condition.operationToken) {
                    KtTokens.EQEQ -> canBeReplacedWithSafeCall(receiverExpression, thenClause, elseClause)
                    KtTokens.EXCLEQ -> canBeReplacedWithSafeCall(receiverExpression, elseClause, thenClause)
                    else -> false
                }
            }
            is KtIsExpression -> {
                val context = element.analyze()
                val receiverExpression = condition.leftHandSide
                if (!receiverExpression.isStableVariable(context)) return false
                val targetType = context[BindingContext.TYPE, condition.typeReference] ?: return false
                if (TypeUtils.isNullableType(targetType)) return false
                val originalType = receiverExpression.getType(context) ?: return false
                if (!targetType.isSubtypeOf(originalType)) return false
                when(condition.isNegated) {
                    true -> canBeReplacedWithSafeCall(receiverExpression, thenClause, elseClause)
                    false -> canBeReplacedWithSafeCall(receiverExpression, elseClause, thenClause)
                }
            }
            else -> false
        }
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition
        val newExpr = when (condition) {
            is KtBinaryExpression -> {
                val receiverExpression = condition.expressionComparedToNull()!!

                val selectorExpression =
                        when(condition.operationToken) {
                            KtTokens.EQEQ -> findSelectorExpressionInClause(element.`else`!!, receiverExpression)!!
                            KtTokens.EXCLEQ -> findSelectorExpressionInClause(element.then!!, receiverExpression)!!
                            else -> throw IllegalArgumentException()
                        }

                KtPsiFactory(element).createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression) as KtSafeQualifiedExpression
            }
            is KtIsExpression -> {
                val typeRef = condition.typeReference!!
                val lhs = condition.leftHandSide
                val selector = if (condition.isNegated) element.`else`!! else element.then!!
                val selectorExpression = findSelectorExpressionInClause(selector, lhs)!!
                KtPsiFactory(element).createExpressionByPattern("($0 as? $1)?.$2", lhs, typeRef, selectorExpression) as KtSafeQualifiedExpression
            }
            else -> null
        }

        if (newExpr != null) {
            val safeAccessExpr = runWriteAction { element.replaced(newExpr) }
            if (editor != null) {
                safeAccessExpr.inlineReceiverIfApplicableWithPrompt(editor)
            }
        }
    }

    private fun canBeReplacedWithSafeCall(receiver: KtExpression, nullClause: KtExpression?, notNullClause: KtExpression?) =
            nullClause?.isNullExpression() ?: true &&
            notNullClause != null && clauseContainsAppropriateDotQualifiedExpression(notNullClause, receiver)

    private fun clauseContainsAppropriateDotQualifiedExpression(clause: KtExpression, receiverExpression: KtExpression)
            = findSelectorExpressionInClause(clause, receiverExpression) != null

    private fun findSelectorExpressionInClause(clause: KtExpression, receiverExpression: KtExpression): KtExpression? {
        val expression = clause.unwrapBlockOrParenthesis() as? KtDotQualifiedExpression ?: return null

        if (expression.receiverExpression.text != receiverExpression.text) return null

        return expression.selectorExpression
    }
}
