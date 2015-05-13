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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.replaced

public class IfThenToSafeAccessInspection : IntentionBasedInspection<JetIfExpression>(IfThenToSafeAccessIntention())

public class IfThenToSafeAccessIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with safe access expression") {

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition() as? JetBinaryExpression ?: return false
        val thenClause = element.getThen()
        val elseClause = element.getElse()

        val receiverExpression = condition.expressionComparedToNull() ?: return false
        if (!receiverExpression.isStableVariable()) return false

        return when (condition.getOperationToken()) {
            JetTokens.EQEQ ->
                thenClause?.isNullExpressionOrEmptyBlock() ?: true &&
                elseClause != null && clauseContainsAppropriateDotQualifiedExpression(elseClause, receiverExpression)

            JetTokens.EXCLEQ ->
                elseClause?.isNullExpressionOrEmptyBlock() ?: true &&
                thenClause != null && clauseContainsAppropriateDotQualifiedExpression(thenClause, receiverExpression)

            else ->
                false
        }
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val safeAccessExpr = applyTo(element)
        safeAccessExpr.inlineReceiverIfApplicableWithPrompt(editor)
    }

    public fun applyTo(element: JetIfExpression): JetSafeQualifiedExpression {
        val condition = element.getCondition() as JetBinaryExpression
        val receiverExpression = condition.expressionComparedToNull()!!

        val selectorExpression =
                when(condition.getOperationToken()) {
                    JetTokens.EQEQ -> findSelectorExpressionInClause(element.getElse()!!, receiverExpression)!!

                    JetTokens.EXCLEQ -> findSelectorExpressionInClause(element.getThen()!!, receiverExpression)!!

                    else -> throw IllegalArgumentException()
                }

        val newExpr = JetPsiFactory(element).createExpressionByPattern("$0?.$1", receiverExpression, selectorExpression) as JetSafeQualifiedExpression
        return element.replaced(newExpr)
    }

    private fun clauseContainsAppropriateDotQualifiedExpression(clause: JetExpression, receiverExpression: JetExpression)
            = findSelectorExpressionInClause(clause, receiverExpression) != null

    private fun findSelectorExpressionInClause(clause: JetExpression, receiverExpression: JetExpression): JetExpression? {
        val expression = clause.unwrapBlockOrParenthesis() as? JetDotQualifiedExpression ?: return null

        if (expression.getReceiverExpression().getText() != receiverExpression.getText()) return null

        return expression.getSelectorExpression()
    }
}
