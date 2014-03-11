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

import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil


public class ConvertNegatedBooleanSequenceIntention : JetSelfTargetingIntention<JetBinaryExpression>(
        "convert.negated.boolean.sequence", javaClass()) {

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getParent() is JetBinaryExpression) return false // operate only on the longest sequence
        var binaryExpression : JetBinaryExpression? = element
        val originalOperator = element.getOperationToken()

        if (!(originalOperator == JetTokens.ANDAND || originalOperator == JetTokens.OROR)) {
            return false
        }

        do {
            val leftChild = binaryExpression?.getLeft()
            val rightChild = binaryExpression?.getRight()
            val operator = binaryExpression?.getOperationToken()
            when {
                rightChild !is JetPrefixExpression,
                operator != originalOperator,
                !(leftChild is JetPrefixExpression || leftChild is JetBinaryExpression) -> return false
                else -> binaryExpression = leftChild as? JetBinaryExpression
            }
        } while (binaryExpression != null)

        return true
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        var binaryExpression = element : JetBinaryExpression?
        var expressionText = ""
        val operator = binaryExpression!!.getOperationToken()
        val operatorText = when(binaryExpression!!.getOperationToken()) {
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException("Invalid operator: '$operator'. Only expressions using '&&' or '||' can be converted.")
        }

        while (binaryExpression != null) {
            val leftChild = binaryExpression!!.getLeft()
            val rightChild = binaryExpression!!.getRight() as JetPrefixExpression
            expressionText = " $operatorText ${rightChild.getBaseExpression()!!.getText()}$expressionText"
            if (leftChild is JetPrefixExpression) {
                val leftChildText = (leftChild as JetPrefixExpression).getBaseExpression()!!.getText()
                expressionText = "$leftChildText$expressionText"
            }
            binaryExpression = leftChild as? JetBinaryExpression
        }

        val newExpression = JetPsiFactory.createExpression(element.getProject(),"!($expressionText)")

        val insertedElement = element.replace(newExpression)
        val insertedElementParent = insertedElement.getParent() as? JetParenthesizedExpression ?: return

        if (JetPsiUtil.areParenthesesUseless(insertedElementParent)) {
            insertedElementParent.replace(insertedElement)
        }
    }

}