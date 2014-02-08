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

public class SwapBinaryExpression : JetSelfTargetingIntention<JetBinaryExpression>(
        "swap.binary.expression", javaClass()
) {
    class object {
        val firstLeft = 1
        val firstRight = 2
        val nextLeft = 3
        val nextRight = 4
    }

    fun getInnermostOperand(element: JetBinaryExpression, position: Int): JetExpression {
        assert (position <= 4 && position >= 1) {
            "Unexpected second argument passed to SwapBinaryExpression.getInnermostOperand"
        }
        val left = element.getLeft()
        val right = element.getRight()

        return when (position) {
            SwapBinaryExpression.firstLeft -> if (left is JetBinaryExpression) getInnermostOperand(left, nextRight) else left as JetExpression
            SwapBinaryExpression.firstRight -> if (right is JetBinaryExpression) getInnermostOperand(right, nextLeft) else right as JetExpression
            SwapBinaryExpression.nextLeft -> if (left is JetBinaryExpression) getInnermostOperand(left, nextLeft) else left as JetExpression
            SwapBinaryExpression.nextRight -> if (right is JetBinaryExpression) getInnermostOperand(right, nextLeft) else right as JetExpression
            else -> null as JetExpression
        }
    }

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
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
        val left = getInnermostOperand(element, SwapBinaryExpression.firstLeft)
        val right = getInnermostOperand(element, SwapBinaryExpression.firstRight)
        val newRight = JetPsiFactory.createExpression(element.getProject(), left.getText())
        val newLeft = JetPsiFactory.createExpression(element.getProject(), right.getText())
        left.replace(newLeft)
        right.replace(newRight)
        element.replace(JetPsiFactory.createBinaryExpression(element.getProject(), element.getLeft(), convertedOperator, element.getRight()))
    }
}
