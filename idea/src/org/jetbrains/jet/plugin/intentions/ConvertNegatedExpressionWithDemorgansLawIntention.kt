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

import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetExpression

public class ConvertNegatedExpressionWithDemorgansLawIntention : JetSelfTargetingIntention<JetPrefixExpression>(
        "convert.negated.expression.with.demorgans.law",javaClass()
) {

    override fun isApplicableTo(element: JetPrefixExpression): Boolean {
        val prefixOperator = element.getOperationReference().getReferencedNameElementType()
        if (prefixOperator != JetTokens.EXCL) return false

        val parenthesizedExpression = element.getBaseExpression() as? JetParenthesizedExpression
        var binaryElement = parenthesizedExpression?.getExpression() as? JetBinaryExpression
        val operatorToken = binaryElement?.getOperationToken() ?: return false

        if (!(operatorToken == JetTokens.ANDAND || operatorToken == JetTokens.OROR)) {
            return false
        }

        while (binaryElement != null) { // While loop lets us handle expressions of any length
            val leftChild = binaryElement?.getLeft()
            val rightChild = binaryElement?.getRight()
            if (rightChild is PsiErrorElementImpl || operatorToken != binaryElement?.getOperationToken()) {
                return false
            }
            binaryElement = leftChild as? JetBinaryExpression
        }

        return true
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        val parenthesizedExpression = element.getBaseExpression() as JetParenthesizedExpression
        var binaryElement = parenthesizedExpression.getExpression() as? JetBinaryExpression
        val operator = binaryElement!!.getOperationToken()
        val operatorText = when(operator){
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException("Invalid operator: \"$operator\" Only expressions using '&&' or '||' can be converted.")
        }
        var expressionText = ""

        while (binaryElement != null) {
            val leftChild = binaryElement?.getLeft()
            val rightChild = binaryElement?.getRight()

            val rightChildText = rightChild!!.getText()
            expressionText = "$operatorText !$rightChildText $expressionText "
            if (leftChild !is JetBinaryExpression) {
                expressionText = "!${leftChild!!.getText()} $expressionText"
            }
            binaryElement = leftChild as? JetBinaryExpression
        }

        val newExpression = JetPsiFactory.createExpression(element.getProject(),expressionText)
        element.replace(newExpression)
    }

}
