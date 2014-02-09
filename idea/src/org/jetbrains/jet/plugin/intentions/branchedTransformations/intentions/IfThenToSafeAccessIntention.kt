/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions

import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiUtil

public class IfThenToSafeAccessIntention: JetSelfTargetingIntention<JetIfExpression>("if.then.to.safe.access", javaClass()) {

    private enum class ConditionalType {
        EQUALS_NULL
        NOT_EQUALS_NULL
    }

    fun getConditionalType (expression: JetBinaryExpression): ConditionalType? {
        val operationToken = expression.getOperationToken()

        val rightIsNull = expression.getRight()?.evaluatesToNull()
        val leftIsNull = expression.getLeft()?.evaluatesToNull()
        if (rightIsNull == null || leftIsNull == null || leftIsNull == rightIsNull)
            return null

        return when (operationToken) {
            JetTokens.EQEQ -> ConditionalType.EQUALS_NULL
            JetTokens.EXCLEQ -> ConditionalType.NOT_EQUALS_NULL
            else -> null
        }
    }

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()
        if (thenClause == null || elseClause == null || condition !is JetBinaryExpression) return false

        val receiverExpression = getReceiverExpression(condition)
        if (receiverExpression == null) return false

        return when (getConditionalType(condition)) {
            ConditionalType.EQUALS_NULL ->
                thenClause.evaluatesToNull() && clauseHasDotQualifiedExpressionForReceiver(elseClause, receiverExpression)

            ConditionalType.NOT_EQUALS_NULL ->
                clauseHasDotQualifiedExpressionForReceiver(thenClause, receiverExpression) && elseClause.evaluatesToNull()

            else -> false

        }
    }

    fun getReceiverExpression(condition: JetBinaryExpression): JetExpression? = when {
            condition.getLeft()?.evaluatesToNull() == false -> condition.getLeft()
            condition.getRight()?.evaluatesToNull() == false -> condition.getRight()
            else -> null
    }


    fun JetExpression.evaluatesToNull(): Boolean {
        val expression = JetPsiUtil.deparenthesize(this)
        return when (expression) {
            is JetBlockExpression -> expression.getStatements().size() == 1 &&
                                     (expression.getStatements().first!! as? JetExpression)?.evaluatesToNull() ?: false
            is JetConstantExpression -> expression.getText() == "null"
            else -> false
        }
    }

    fun clauseHasDotQualifiedExpressionForReceiver(clause: JetElement, receiverExpression: JetExpression): Boolean =
        getSelectorExpressionFromClause(clause as JetExpression, receiverExpression) != null

    fun getSelectorExpressionFromClause(clause: JetExpression, receiverExpression: JetExpression): JetExpression? {
        val expression = JetPsiUtil.deparenthesize(clause)
        when (expression) {
            is JetBlockExpression ->
                if (expression.getStatements().size() == 1) {
                    val firstExpression = expression.getStatements().first!! as? JetExpression
                    if (firstExpression is JetExpression)
                        return getSelectorExpressionFromClause(firstExpression, receiverExpression)
                }
            is JetDotQualifiedExpression ->
                if (expression.getReceiverExpression().getText() == receiverExpression.getText()){
                    return expression.getSelectorExpression()
                }
        }
        return null
    }


    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression
        val receiverExpression = checkNotNull(getReceiverExpression(condition), "The receiver expression cannot be null")
        val thenClause = checkNotNull(element.getThen(), "The then clause cannot be null")
        val elseClause = checkNotNull(element.getElse(), "The else clause cannot be null")
        val conditionType = checkNotNull(getConditionalType(condition), "The condition type cannot be null")

        val selectorExpression = when(conditionType) {
            ConditionalType.EQUALS_NULL -> getSelectorExpressionFromClause(elseClause, receiverExpression)
            ConditionalType.NOT_EQUALS_NULL -> getSelectorExpressionFromClause(thenClause, receiverExpression)
        }

        val resultingExprString = "${receiverExpression?.getText()}?.${selectorExpression?.getText()}"
        element.replace(JetPsiFactory.createExpression(element.getProject(),resultingExprString))
    }

}
