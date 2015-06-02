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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.LinkedList

public class ConvertNegatedExpressionWithDemorgansLawIntention : JetSelfTargetingOffsetIndependentIntention<JetPrefixExpression>(javaClass(), "DeMorgan Law") {

    override fun isApplicableTo(element: JetPrefixExpression): Boolean {
        val prefixOperator = element.getOperationReference().getReferencedNameElementType()
        if (prefixOperator != JetTokens.EXCL) return false

        val parenthesizedExpression = element.getBaseExpression() as? JetParenthesizedExpression
        val baseExpression = parenthesizedExpression?.getExpression() as? JetBinaryExpression ?: return false

        when (baseExpression.getOperationToken()) {
            JetTokens.ANDAND -> setText("Replace '&&' with '||'")
            JetTokens.OROR -> setText("Replace '||' with '&&'")
            else -> return false
        }

        val elements = splitBooleanSequence(baseExpression) ?: return false
        return elements.none { it is PsiErrorElementImpl }
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        val parenthesizedExpression = element.getBaseExpression() as JetParenthesizedExpression
        val baseExpression = parenthesizedExpression.getExpression() as JetBinaryExpression

        val operatorText = when (baseExpression.getOperationToken()) {
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException()
        }

        val operands = splitBooleanSequence(baseExpression)!!.reverse()

        val newExpression = JetPsiFactory(element).buildExpression {
            for ((i, operand) in operands.withIndex()) {
                if (i > 0) {
                    appendFixedText(operatorText)
                }
                appendExpression(operand.negate())
            }
        }

        element.replace(newExpression)
    }

    private fun splitBooleanSequence(expression: JetBinaryExpression): List<JetExpression>? {
        val result = LinkedList<JetExpression>()
        val firstOperator = expression.getOperationToken()

        var currentItem: JetExpression? = expression
        while (currentItem as? JetBinaryExpression != null) {
            val remainingExpression = currentItem as JetBinaryExpression
            val operation = remainingExpression.getOperationToken()
            if (!(operation == JetTokens.ANDAND || operation == JetTokens.OROR)) break

            if (operation != firstOperator) return null //Boolean sequence must be homogenous

            val leftChild = remainingExpression.getLeft()
            val rightChild = remainingExpression.getRight()
            result.add(rightChild)
            currentItem = leftChild
        }

        result.addIfNotNull(currentItem)
        return result
    }

}
