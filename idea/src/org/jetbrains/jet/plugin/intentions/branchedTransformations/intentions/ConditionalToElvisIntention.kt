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

import org.jetbrains.jet.lang.psi.JetSafeQualifiedExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetElement

public class ConditionalToElvisIntention: JetSelfTargetingIntention<JetIfExpression>("conditional.to.elvis", javaClass()) {

    enum class ConditionalType {
        LHS_EQUALS_NULL
        RHS_EQUALS_NULL
        LHS_NOT_EQUALS_NULL
        RHS_NOT_EQUALS_NULL
    }

    fun getConditionalType (expression: JetBinaryExpression): ConditionalType? {
        val operationText = expression.getOperationReference().getText()

        val rightIsNull = expression.getRight()?.evaluatesToNull()
        val leftIsNull = expression.getLeft()?.evaluatesToNull()

        if (rightIsNull == true && leftIsNull == false){
           if (operationText == "==") return ConditionalType.LHS_EQUALS_NULL
           if (operationText == "!=") return ConditionalType.LHS_NOT_EQUALS_NULL
        }
        else if (leftIsNull == true && rightIsNull == false){
            if (operationText == "==") return ConditionalType.RHS_EQUALS_NULL
            if (operationText == "!=") return ConditionalType.RHS_NOT_EQUALS_NULL
        }
        return null
    }

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenClause = element.getThen()
        val elseClause = element.getElse()
        if ( condition is JetBinaryExpression) {
            val receiverExpression = getReceiverExpression(condition)
            val conditionalType = getConditionalType(condition)
            if (receiverExpression is JetExpression) {
                if (conditionalType == ConditionalType.LHS_EQUALS_NULL ||
                    conditionalType == ConditionalType.RHS_EQUALS_NULL) {
                    return (thenClause?.evaluatesToNull()?:true) &&
                           (elseClause != null && clauseHasDotQualifiedExpressionForReceiver(elseClause, receiverExpression))
                } else if (conditionalType == ConditionalType.LHS_NOT_EQUALS_NULL ||
                           conditionalType == ConditionalType.RHS_NOT_EQUALS_NULL) {
                    return (thenClause != null && clauseHasDotQualifiedExpressionForReceiver(thenClause, receiverExpression)) &&
                           (elseClause?.evaluatesToNull()?:true)
                }
            }
        }
        return false
    }

    fun getReceiverExpression(condition: JetBinaryExpression): JetExpression? {
        if (condition.getLeft()?.evaluatesToNull() == false) {
            return condition.getLeft()
        }
        else if (condition.getRight()?.evaluatesToNull() == false) {
            return condition.getRight()
        }
        else return null
    }


    fun JetElement.evaluatesToNull(): Boolean {
        if (this is JetBlockExpression) {
            if (this.getStatements().isEmpty()) {
                return true
            } else if (this.getStatements().size() == 1) {
                return this.getStatements().first?.evaluatesToNull()?:false
            } else {
                return false
            }
        } else if (this is JetConstantExpression) {
            return this.getText() == "null"
        } else {
            return false
        }
    }

    fun clauseHasDotQualifiedExpressionForReceiver(clause: JetElement, receiverExpression: JetExpression): Boolean =
        getSelectorExpressionFromClause(clause, receiverExpression) != null

    fun getSelectorExpressionFromClause(clause: JetElement, receiverExpression: JetExpression): JetExpression? {
        if (clause is JetBlockExpression) {
            if (clause.getStatements().isEmpty()) {
                return null
            } else if (clause.getStatements().size() == 1) {
                return getSelectorExpressionFromClause(clause.getStatements().first!!, receiverExpression)
            } else {
                return null
            }
        }
        else if (clause is JetDotQualifiedExpression) {
            if (clause.getReceiverExpression().javaClass == receiverExpression.javaClass &&
                clause.getReceiverExpression().getText() == receiverExpression.getText()){
                return clause.getSelectorExpression()
            }
        }
        return null
    }


    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition() as JetBinaryExpression
        val receiverExpression = getReceiverExpression(condition)
        assert(receiverExpression != null, "The receiver expression cannot be null")

        val conditionType = getConditionalType(condition)
        val selectorExpression = if (conditionType == ConditionalType.LHS_EQUALS_NULL ||
                                     conditionType == ConditionalType.RHS_EQUALS_NULL) {
            assert(element.getElse() != null, "The else clause cannot be null")
            getSelectorExpressionFromClause(element.getElse()!!, receiverExpression!!)
        } else {
            assert(element.getThen() != null, "The then clause cannot be null")
            getSelectorExpressionFromClause(element.getThen()!!, receiverExpression!!)
        }

        val resultingExprString = "${receiverExpression?.getText()}?.${selectorExpression?.getText()}"
        element.replace(JetPsiFactory.createExpression(element.getProject(),resultingExprString))
    }

}
