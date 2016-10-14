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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class IfThenToElvisInspection : IntentionBasedInspection<KtIfExpression>(IfThenToElvisIntention::class)

class IfThenToElvisIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(
        KtIfExpression::class.java,
        "Replace 'if' expression with elvis expression"
) {

    private fun KtExpression.clausesReplaceableByElvis(firstClause: KtExpression, secondClause: KtExpression) =
            firstClause.isNotNullExpression() && secondClause.evaluatesTo(this) &&
            !(firstClause is KtThrowExpression && firstClause.throwsNullPointerExceptionWithNoArguments())

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        val condition = element.condition as? KtOperationExpression ?: return false
        val thenClause = element.then ?: return false
        val elseClause = element.`else` ?: return false

        val expression = when (condition) {
            is KtBinaryExpression -> condition.expressionComparedToNull() ?: return false
            is KtIsExpression -> condition.leftHandSide
            else -> return false

        }
        if (!expression.isStableVariable()) return false

        return when (condition) {
            is KtBinaryExpression -> when (condition.operationToken) {
                KtTokens.EQEQ -> expression.clausesReplaceableByElvis(thenClause, elseClause)
                KtTokens.EXCLEQ -> expression.clausesReplaceableByElvis(elseClause, thenClause)
                else -> false
            }
            is KtIsExpression -> when (condition.isNegated) {
                true -> expression.clausesReplaceableByElvis(thenClause, elseClause)
                false -> expression.clausesReplaceableByElvis(elseClause, thenClause)
            }
            else -> false
        }
    }

    private fun KtExpression.isNotNullExpression(): Boolean {
        val innerExpression = this.unwrapBlockOrParenthesis()
        return innerExpression !is KtBlockExpression && innerExpression.node.elementType != KtNodeTypes.NULL
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition as KtOperationExpression

        val thenClause = element.then!!
        val elseClause = element.`else`!!
        val thenExpression = thenClause.unwrapBlockOrParenthesis()
        val elseExpression = elseClause.unwrapBlockOrParenthesis()

        val (left, right) = when (condition) {
            is KtBinaryExpression -> when (condition.operationToken) {
                KtTokens.EQEQ -> Pair(elseExpression, thenExpression)
                KtTokens.EXCLEQ -> Pair(thenExpression, elseExpression)
                else -> throw IllegalArgumentException()
            }
            is KtIsExpression -> when (condition.isNegated) {
                true -> Pair(elseExpression, thenExpression)
                false -> Pair(thenExpression, elseExpression)
            }
            else -> throw IllegalArgumentException()
        }

        val factory = KtPsiFactory(element)
        val newExpr = factory.createExpressionByPattern("$0 ?: $1", left, right) as KtBinaryExpression
        if (condition is KtIsExpression) {
            newExpr.left!!.replace(factory.createExpressionByPattern("$0 as? $1", left, condition.typeReference!!))
        }
        val elvis = KtPsiUtil.deparenthesize(element.replaced(newExpr)) as KtBinaryExpression

        if (editor != null) {
            elvis.inlineLeftSideIfApplicableWithPrompt(editor)
        }
    }
}
