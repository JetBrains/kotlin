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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.intentions.SwapBinaryExpression.Position.*
import org.jetbrains.jet.plugin.util.JetPsiPrecedences

public class SwapBinaryExpression : JetSelfTargetingIntention<JetBinaryExpression>(
        "swap.binary.expression", javaClass()
) {
    enum class Position {
        FIRST_LEFT
        FIRST_RIGHT
        NEXT_LEFT
        NEXT_RIGHT
    }

    fun getInnermostOperand(element: JetBinaryExpression, position: Position): JetExpression? {
        val left = element.getLeft()
        val right = element.getRight()
        if (left == null || right == null) {
            return null
        }

        val parentPrecedence = JetPsiPrecedences.getPrecedence(element)
        val leftPrecedence = JetPsiPrecedences.getPrecedence(left)
        val rightPrecedence = JetPsiPrecedences.getPrecedence(right)
        return when (position) {
            FIRST_LEFT -> if (leftPrecedence < parentPrecedence) left
                else if (left is JetBinaryExpression) getInnermostOperand(left, NEXT_RIGHT)
                else left as JetExpression
            FIRST_RIGHT -> if (rightPrecedence < parentPrecedence) right
                else if (right is JetBinaryExpression) getInnermostOperand(right, NEXT_LEFT)
                else right as JetExpression
            NEXT_LEFT -> if (leftPrecedence < parentPrecedence || left !is JetBinaryExpression) left
                else getInnermostOperand(left, NEXT_LEFT)
            NEXT_RIGHT -> if (rightPrecedence < parentPrecedence || right !is JetBinaryExpression) right
                else getInnermostOperand(right, NEXT_RIGHT)
        }
    }

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (getInnermostOperand(element, FIRST_LEFT) == null || getInnermostOperand(element, FIRST_RIGHT) == null) {
            return false
        }

        val operatorTextLiteral = element.getOperationReference().getText()
        val approvedOperators = setOf("+", "plus", "*", "times", "||", "or", "and",
                                      "&&", "==", "!=", "xor", ">", "<", ">=", "<=", "equals")

        if (approvedOperators.contains(operatorTextLiteral)) {
            setText("Flip '$operatorTextLiteral'")
            return true
        }
        return false
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val operator = element.getOperationReference().getText()!!
        val convertedOperator = when (operator) {
            ">" -> "<"
            "<" -> ">"
            "<=" -> ">="
            ">=" -> "<="
            else -> operator
        }
        val left = getInnermostOperand(element, FIRST_LEFT)!!
        val right = getInnermostOperand(element, FIRST_RIGHT)!!
        val newRight = JetPsiFactory.createExpression(element.getProject(), left.getText())
        val newLeft = JetPsiFactory.createExpression(element.getProject(), right.getText())
        left.replace(newLeft)
        right.replace(newRight)
        element.replace(JetPsiFactory.createBinaryExpression(element.getProject(), element.getLeft(), convertedOperator, element.getRight()))
    }
}
