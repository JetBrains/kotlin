/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.util.hasComments
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ControlFlowWithEmptyBodyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitIfExpression(expression: KtIfExpression) {
            if (expression.then.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, expression.ifKeyword)
            }
            val elseKeyword = expression.elseKeyword
            if (elseKeyword != null && expression.`else`.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, elseKeyword)
            }
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (expression.entries.isNotEmpty()) return
            holder.registerProblem(expression, expression.whenKeyword)
        }

        override fun visitForExpression(expression: KtForExpression) {
            if (expression.body.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, expression.forKeyword)
            }
        }

        override fun visitWhileExpression(expression: KtWhileExpression) {
            val keyword = expression.allChildren.firstOrNull { it.node.elementType == KtTokens.WHILE_KEYWORD } ?: return
            val body = expression.body
            if (body?.hasComments() != true && body.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, keyword)
            }
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            val keyword = expression.allChildren.firstOrNull { it.node.elementType == KtTokens.DO_KEYWORD } ?: return
            val body = expression.body
            if (body?.hasComments() != true && body.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, keyword)
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            val callee = expression.calleeExpression ?: return
            if (!expression.isCalling(controlFlowFunctions)) return
            val body = when (val argument = expression.valueArguments.singleOrNull()?.getArgumentExpression()) {
                is KtLambdaExpression -> argument.bodyExpression
                is KtNamedFunction -> argument.bodyBlockExpression
                else -> return
            }
            if (body.isEmptyBodyOrNull()) {
                holder.registerProblem(expression, callee)
            }
        }
    }

    private fun KtExpression?.isEmptyBodyOrNull(): Boolean = if (this == null) true else this is KtBlockExpression && statements.isEmpty()

    private fun ProblemsHolder.registerProblem(expression: KtExpression, keyword: PsiElement) {
        val keywordText = if (expression is KtDoWhileExpression) "do while" else keyword.text
        registerProblem(
            expression,
            keyword.textRange.shiftLeft(expression.startOffset),
            "'$keywordText' has empty body"
        )
    }

    companion object {
        private val controlFlowFunctions = listOf("kotlin.also").map { FqName(it) }
    }
}