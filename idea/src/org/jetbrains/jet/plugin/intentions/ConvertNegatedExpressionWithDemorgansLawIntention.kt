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
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.LinkedList
import org.jetbrains.jet.plugin.JetBundle

public class ConvertNegatedExpressionWithDemorgansLawIntention : JetSelfTargetingIntention<JetPrefixExpression>(
        "convert.negated.expression.with.demorgans.law", javaClass()
) {

    override fun isApplicableTo(element: JetPrefixExpression): Boolean {
        val prefixOperator = element.getOperationReference().getReferencedNameElementType()
        if (prefixOperator != JetTokens.EXCL) return false

        val parenthesizedExpression = element.getBaseExpression() as? JetParenthesizedExpression
        val baseExpression = parenthesizedExpression?.getExpression() as? JetBinaryExpression ?: return false

        when (baseExpression.getOperationToken()) {
            JetTokens.ANDAND -> setText(JetBundle.message("convert.negated.expression.with.demorgans.law.andToOr"))
            JetTokens.OROR -> setText(JetBundle.message("convert.negated.expression.with.demorgans.law.orToAnd"))
            else -> return false
        }

        val elements = splitBooleanSequence(baseExpression) ?: return false
        return !(elements.any { x -> x is PsiErrorElementImpl })
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        val parenthesizedExpression = element.getBaseExpression() as JetParenthesizedExpression
        val baseExpression = parenthesizedExpression.getExpression() as JetBinaryExpression
        val operatorText = when (baseExpression.getOperationToken()) {
            JetTokens.ANDAND -> JetTokens.OROR.getValue()
            JetTokens.OROR -> JetTokens.ANDAND.getValue()
            else -> throw IllegalArgumentException(
                    "Invalid operator: '${baseExpression.getOperationToken()}'. Only expressions using '&&' or '||' can be converted.")
        }
        val elements = splitBooleanSequence(baseExpression)
        val negatedElements = elements!!.map { exp -> handleSpecial(exp) }
        val negatedExpression = negatedElements.subList(0, negatedElements.lastIndex).foldRight(
                "${negatedElements.last()}", { negated, exp -> "$exp $operatorText $negated" })

        val newExpression = JetPsiFactory(element).createExpression(negatedExpression)
        element.replace(newExpression)
    }

    fun handleSpecial(expression: JetExpression): String {
        return when (expression) {
            is JetSimpleNameExpression, is JetConstantExpression, is JetPrefixExpression,
            is JetParenthesizedExpression -> "!${expression.getText()}"
            else -> "!(${expression.getText()})"
        }
    }

    fun splitBooleanSequence(expression: JetBinaryExpression): List<JetExpression>? {
        val itemList = LinkedList<JetExpression>()
        val firstOperator = expression.getOperationToken()
        var currentItem: JetExpression? = expression

        while (currentItem as? JetBinaryExpression != null) {
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
