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
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.core.replaced

public class IfThenToSafeAccessInspection : IntentionBasedInspection<KtIfExpression>(IfThenToSafeAccessIntention())

public class IfThenToSafeAccessIntention : JetSelfTargetingOffsetIndependentIntention<KtIfExpression>(javaClass(), "Replace 'if' expression with safe access expression") {

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        val condition = element.getCondition() as? KtBinaryExpression ?: return false
        val thenClause = element.getThen()
        val elseClause = element.getElse()

        val receiverExpression = condition.expressionComparedToNull() ?: return false
        if (!receiverExpression.isStableVariable()) return false

        return when (condition.getOperationToken()) {
            KtTokens.EQEQ ->
                thenClause?.isNullExpressionOrEmptyBlock() ?: true &&
                elseClause != null && clauseContainsAppropriateDotQualifiedExpression(elseClause, receiverExpression)

            KtTokens.EXCLEQ ->
                elseClause?.isNullExpressionOrEmptyBlock() ?: true &&
                thenClause != null && clauseContainsAppropriateDotQualifiedExpression(thenClause, receiverExpression)

            else ->
                false
        }
    }

    override fun applyTo(element: KtIfExpression, editor: Editor) {
        val safeAccessExpr = applyTo(element)
        safeAccessExpr.inlineReceiverIfApplicableWithPrompt(editor)
    }

    public fun applyTo(element: KtIfExpression): KtSafeQualifiedExpression {
        val condition = element.getCondition() as KtBinaryExpression
        val receiverExpression = condition.expressionComparedToNull()!!

        val selectorExpression =
                when(condition.getOperationToken()) {
                    KtTokens.EQEQ -> findSelectorExpressionInClause(element.getElse()!!, receiverExpression)!!

                    KtTokens.EXCLEQ -> findSelectorExpressionInClause(element.getThen()!!, receiverExpression)!!

                    else -> throw IllegalArgumentException()
                }

        val newExpr = KtPsiFactory(element).createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression) as KtSafeQualifiedExpression
        return element.replaced(newExpr)
    }

    private fun clauseContainsAppropriateDotQualifiedExpression(clause: KtExpression, receiverExpression: KtExpression)
            = findSelectorExpressionInClause(clause, receiverExpression) != null

    private fun findSelectorExpressionInClause(clause: KtExpression, receiverExpression: KtExpression): KtExpression? {
        val expression = clause.unwrapBlockOrParenthesis() as? KtDotQualifiedExpression ?: return null

        if (expression.getReceiverExpression().getText() != receiverExpression.getText()) return null

        return expression.getSelectorExpression()
    }
}
