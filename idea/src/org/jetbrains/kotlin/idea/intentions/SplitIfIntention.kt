/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.core.util.start
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class SplitIfIntention : SelfTargetingIntention<KtExpression>(KtExpression::class.java, KotlinBundle.lazyMessage("split.if.into.two")) {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        return when (element) {
            is KtOperationReferenceExpression -> isOperatorValid(element)
            is KtIfExpression -> getFirstValidOperator(element) != null && element.ifKeyword.textRange.containsOffset(caretOffset)
            else -> false
        }
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        val operator = when (element) {
            is KtIfExpression -> getFirstValidOperator(element)!!
            else -> element as KtOperationReferenceExpression
        }

        val ifExpression = operator.getNonStrictParentOfType<KtIfExpression>()

        val commentSaver = CommentSaver(ifExpression!!)

        val expression = operator.parent as KtBinaryExpression
        val rightExpression = KtPsiUtil.safeDeparenthesize(getRight(expression, ifExpression.condition!!, commentSaver))
        val leftExpression = KtPsiUtil.safeDeparenthesize(expression.left!!)
        val thenBranch = ifExpression.then!!
        val elseBranch = ifExpression.`else`

        val psiFactory = KtPsiFactory(element)

        val innerIf = psiFactory.createIf(rightExpression, thenBranch, elseBranch)

        val newIf = when (operator.getReferencedNameElementType()) {
            KtTokens.ANDAND -> psiFactory.createIf(leftExpression, psiFactory.createSingleStatementBlock(innerIf), elseBranch)

            KtTokens.OROR -> {
                val container = ifExpression.parent

                // special case
                if (container is KtBlockExpression && elseBranch == null && thenBranch.lastBlockStatementOrThis().isExitStatement()) {
                    val secondIf = container.addAfter(innerIf, ifExpression)
                    container.addAfter(psiFactory.createNewLine(), ifExpression)
                    val firstIf = ifExpression.replace(psiFactory.createIf(leftExpression, thenBranch))
                    commentSaver.restore(PsiChildRange(firstIf, secondIf))
                    return
                } else {
                    psiFactory.createIf(leftExpression, thenBranch, innerIf)
                }
            }

            else -> throw IllegalArgumentException()
        }

        val result = ifExpression.replace(newIf)
        commentSaver.restore(result)
    }

    private fun getRight(element: KtBinaryExpression, condition: KtExpression, commentSaver: CommentSaver): KtExpression {
        //gets the textOffset of the right side of the JetBinaryExpression in context to condition
        val conditionRange = condition.range
        val startOffset = element.right!!.startOffset - conditionRange.start
        val endOffset = conditionRange.length
        val rightString = condition.text.substring(startOffset, endOffset)

        val expression = KtPsiFactory(element).createExpression(rightString)
        commentSaver.elementCreatedByText(expression, condition, TextRange(startOffset, endOffset))
        return expression
    }

    private fun getFirstValidOperator(element: KtIfExpression): KtOperationReferenceExpression? {
        val condition = element.condition ?: return null
        return PsiTreeUtil.findChildrenOfType(condition, KtOperationReferenceExpression::class.java)
            .firstOrNull { isOperatorValid(it) }
    }

    private fun isOperatorValid(element: KtOperationReferenceExpression): Boolean {
        val operator = element.getReferencedNameElementType()
        if (operator != KtTokens.ANDAND && operator != KtTokens.OROR) return false

        var expression = element.parent as? KtBinaryExpression ?: return false

        if (expression.right == null || expression.left == null) return false

        while (true) {
            expression = expression.parent as? KtBinaryExpression ?: break
            if (expression.operationToken != operator) return false
        }

        val ifExpression = expression.parent.parent as? KtIfExpression ?: return false

        if (ifExpression.condition == null) return false
        if (!PsiTreeUtil.isAncestor(ifExpression.condition, element, false)) return false

        if (ifExpression.then == null) return false

        return true
    }
}

