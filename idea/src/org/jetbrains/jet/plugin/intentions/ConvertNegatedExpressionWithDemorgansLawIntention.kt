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
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.ArrayList

public class ConvertNegatedExpressionWithDemorgansLawIntention : JetSelfTargetingIntention<JetPrefixExpression>(
        "convert.negated.expression.with.demorgans.law",javaClass()
) {

    override fun isApplicableTo(element: JetPrefixExpression): Boolean {
        val prefixOperator = element.getOperationReference().getReferencedNameElementType()
        if (prefixOperator != JetTokens.EXCL) return false

        val parenthesizedExpression = element.getBaseExpression() as? JetParenthesizedExpression
        val baseExpression = parenthesizedExpression?.getExpression() as? JetBinaryExpression ?: return false
        val operatorToken = baseExpression.getOperationToken() ?: return false

        if (!(operatorToken == JetTokens.ANDAND || operatorToken == JetTokens.OROR)) {
            return false
        }

        val elements = splitBooleanSequence(baseExpression) ?: return false
        return !(elements.any { x -> x is PsiErrorElementImpl })
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        val parenthesizedExpression = element.getBaseExpression() as JetParenthesizedExpression
        val baseExpression = parenthesizedExpression.getExpression() as JetBinaryExpression
        val operatorText = when(baseExpression.getOperationToken()){
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException(
                    "Invalid operator: '${baseExpression.getOperationToken()}'. Only expressions using '&&' or '||' can be converted.")
        }
        val elements = splitBooleanSequence(baseExpression)
        val negatedElements = elements!!.map { exp -> handleSpecial(exp) }
        val negatedExpression = negatedElements.subList(0,negatedElements.size()-1).foldRight(
                "${negatedElements.last()}",{negated, exp-> "$exp $operatorText $negated"})

        val newExpression = JetPsiFactory.createExpression(element.getProject(),negatedExpression)
        element.replace(newExpression)
    }

    fun handleSpecial(expression: JetExpression) : String {
        return when(expression) {
            JetTokens.TRUE_KEYWORD -> JetTokens.FALSE_KEYWORD.getValue()
            JetTokens.FALSE_KEYWORD -> JetTokens.TRUE_KEYWORD.getValue()
            is JetSimpleNameExpression, is JetConstantExpression, is JetPrefixExpression,
            is JetParenthesizedExpression -> "!${expression.getText()}"
            else -> "!(${expression.getText()})"
        }
    }

    fun splitBooleanSequence(expression: JetBinaryExpression) : List<JetExpression>? {
        val itemList = ArrayList<JetExpression>()
        val firstOperator = expression.getOperationToken()
        var currentItem : JetExpression? = expression

        while(currentItem as? JetBinaryExpression != null) {
            val remainingExpression = currentItem as JetBinaryExpression
            val operation = remainingExpression.getOperationToken()
            if (!(operation == JetTokens.ANDAND || operation == JetTokens.OROR)) break

            if (operation != firstOperator) return null //Boolean sequence must be homogenous

            val leftChild = remainingExpression.getLeft()
            val rightChild = remainingExpression.getRight()
            itemList.add(rightChild as JetExpression)
            currentItem = leftChild
        }

        if (currentItem != null) itemList.add(currentItem!!)
        return itemList
    }

}
