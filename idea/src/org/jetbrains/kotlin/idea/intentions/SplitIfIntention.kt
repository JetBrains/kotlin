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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.conversion.copy.length
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.conversion.copy.start
import org.jetbrains.kotlin.idea.core.CommentSaver
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class SplitIfIntention : JetSelfTargetingIntention<JetExpression>(javaClass(), "Split if into 2 if's") {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        return when (element) {
            is JetSimpleNameExpression -> isOperatorValid(element)
            is JetIfExpression -> getFirstValidOperator(element) != null && element.getIfKeyword().getTextRange().containsOffset(caretOffset)
            else -> false
        }
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val operator = when (element) {
            is JetIfExpression -> getFirstValidOperator(element)!!
            else -> element as JetSimpleNameExpression
        }

        val ifExpression = operator.getNonStrictParentOfType<JetIfExpression>()

        val commentSaver = CommentSaver(ifExpression!!)

        val expression = operator.getParent() as JetBinaryExpression
        val rightExpression = JetPsiUtil.safeDeparenthesize(getRight(expression, ifExpression.getCondition()!!, commentSaver))
        val leftExpression = JetPsiUtil.safeDeparenthesize(expression.getLeft()!!)
        val thenBranch = ifExpression.getThen()!!
        val elseBranch = ifExpression.getElse()

        val psiFactory = JetPsiFactory(element)

        val innerIf = psiFactory.createIf(rightExpression, thenBranch, elseBranch)

        val newIf = when (operator.getReferencedNameElementType()) {
            JetTokens.ANDAND -> psiFactory.createIf(leftExpression, psiFactory.createSingleStatementBlock(innerIf), elseBranch)

            JetTokens.OROR -> {
                val container = ifExpression.getParent()

                if (container is JetBlockExpression && elseBranch == null && thenBranch.lastBlockStatementOrThis().isExitStatement()) { // special case
                    val secondIf = container.addAfter(innerIf, ifExpression)
                    container.addAfter(psiFactory.createNewLine(), ifExpression)
                    val firstIf = ifExpression.replace(psiFactory.createIf(leftExpression, thenBranch))
                    commentSaver.restore(PsiChildRange(firstIf, secondIf))
                    return
                }
                else {
                    psiFactory.createIf(leftExpression, thenBranch, innerIf)
                }
            }

            else -> throw IllegalArgumentException()
        }

        val result = ifExpression.replace(newIf)
        commentSaver.restore(result)
    }

    private fun getRight(element: JetBinaryExpression, condition: JetExpression, commentSaver: CommentSaver): JetExpression {
        //gets the textOffset of the right side of the JetBinaryExpression in context to condition
        val conditionRange = condition.range
        val startOffset = element.getRight()!!.startOffset - conditionRange.start
        val endOffset = conditionRange.length
        val rightString = condition.getText().substring(startOffset, endOffset)

        val expression = JetPsiFactory(element).createExpression(rightString)
        commentSaver.elementCreatedByText(expression, condition, TextRange(startOffset, endOffset))
        return expression
    }

    private fun getFirstValidOperator(element: JetIfExpression): JetSimpleNameExpression? {
        val condition = element.getCondition() ?: return null
        return PsiTreeUtil.findChildrenOfType(condition, javaClass<JetSimpleNameExpression>())
                .firstOrNull { isOperatorValid(it) }
    }

    private fun isOperatorValid(element: JetSimpleNameExpression): Boolean {
        val operator = element.getReferencedNameElementType()
        if (operator != JetTokens.ANDAND && operator != JetTokens.OROR) return false

        var expression = element.getParent() as? JetBinaryExpression ?: return false

        if (expression.getRight() == null || expression.getLeft() == null) return false

        while (true) {
            expression = expression.getParent() as? JetBinaryExpression ?: break
            if (expression.getOperationToken() != operator) return false
        }

        val ifExpression = expression.getParent()?.getParent() as? JetIfExpression ?: return false

        if (ifExpression.getCondition() == null) return false
        if (!PsiTreeUtil.isAncestor(ifExpression.getCondition(), element, false)) return false

        if (ifExpression.getThen() == null) return false

        return true
    }
}

