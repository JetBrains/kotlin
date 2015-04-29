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
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import java.util.LinkedList


public class ConvertNegatedBooleanSequenceIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(
        javaClass(), "Replace negated sequence with DeMorgan equivalent") {

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getParent() is JetBinaryExpression) return false // operate only on the longest sequence
        val opToken = element.getOperationToken()
        if (opToken != JetTokens.ANDAND && opToken != JetTokens.OROR) return false

        return splitBooleanSequence(element) != null
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val operatorText = when(element.getOperationToken()) {
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException() // checked in isApplicableTo
        }

        val elements = splitBooleanSequence(element)!!
        val bareExpressions = elements.map { prefixExpression -> prefixExpression.getBaseExpression()!!.getText() }
        val negatedExpression = bareExpressions.subList(0, bareExpressions.lastIndex).foldRight(
                "!(${bareExpressions.last()}", { negated, expression -> "$expression $operatorText $negated" }
        )

        val newExpression = JetPsiFactory(element).createExpression("$negatedExpression)")

        val insertedElement = element.replace(newExpression)
        val insertedElementParent = insertedElement.getParent() as? JetParenthesizedExpression ?: return

        if (JetPsiUtil.areParenthesesUseless(insertedElementParent)) {
            insertedElementParent.replace(insertedElement)
        }
    }

    private fun splitBooleanSequence(expression: JetBinaryExpression): List<JetPrefixExpression>? {
        val itemList = LinkedList<JetPrefixExpression>()
        val firstOperator = expression.getOperationToken()

        var currentItem: JetBinaryExpression? = expression
        while (currentItem != null) {
            if (currentItem.getOperationToken() != firstOperator) return null //Boolean sequence must be homogeneous

            val rightChild = currentItem.getRight() as? JetPrefixExpression ?: return null
            itemList.add(rightChild)

            val leftChild = currentItem.getLeft()
            when (leftChild) {
                is JetPrefixExpression -> itemList.add(leftChild)
                !is JetBinaryExpression -> return null
            }

            currentItem = leftChild as? JetBinaryExpression
        }

        return itemList
    }

}
