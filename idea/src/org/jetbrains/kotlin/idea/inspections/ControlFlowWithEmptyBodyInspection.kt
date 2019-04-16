/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ControlFlowWithEmptyBodyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitIfExpression(expression: KtIfExpression) {
            if (expression.then.isEmptyBody()) {
                val fix = if (expression.hasEmptyBranches()) RemoveElementFix() else null
                holder.registerProblem(expression, expression.ifKeyword, fix)
            }
            val elseKeyword = expression.elseKeyword
            if (elseKeyword != null && expression.`else`.isEmptyBody()) {
                holder.registerProblem(expression, elseKeyword, RemoveElementFix(isIfElseBranch = true))
            }
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (expression.entries.isNotEmpty() || expression.hasComment()) return
            holder.registerProblem(expression, expression.whenKeyword)
        }

        override fun visitForExpression(expression: KtForExpression) {
            if (expression.body.isEmptyBody()) {
                holder.registerProblem(expression, expression.forKeyword)
            }
        }

        override fun visitWhileExpression(expression: KtWhileExpression) {
            val keyword = expression.allChildren.firstOrNull { it.node.elementType == KtTokens.WHILE_KEYWORD } ?: return
            if (expression.body.isEmptyBody()) {
                holder.registerProblem(expression, keyword)
            }
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            val keyword = expression.allChildren.firstOrNull { it.node.elementType == KtTokens.DO_KEYWORD } ?: return
            if (expression.body.isEmptyBody()) {
                holder.registerProblem(expression, keyword)
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            val callee = expression.calleeExpression ?: return
            if (!expression.isCalling(controlFlowFunctions)) return
            val body = expression.lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression
            if (body.isEmptyBody()) {
                holder.registerProblem(expression, callee)
            }
        }
    }
    
    private fun KtIfExpression.hasEmptyBranches(): Boolean {
        if (!then.isEmptyBody()) return false
        val elseExpression = this.`else` ?: return true
        return if (elseExpression is KtIfExpression) {
            elseExpression.hasEmptyBranches()
        } else {
            elseExpression.isEmptyBody()
        }
    }

    private fun KtExpression?.isEmptyBody(): Boolean {
        if (this == null) return true
        if (this is KtBlockExpression && statements.isEmpty() && !hasComment()) return true
        return false
    }

    private fun KtExpression.hasComment(): Boolean {
        return this.allChildren.any { it is PsiComment }
    }

    private fun ProblemsHolder.registerProblem(expression: KtExpression, keyword: PsiElement, fix: LocalQuickFix? = RemoveElementFix()) {
        val keywordText = if (expression is KtDoWhileExpression) "do while" else keyword.text
        registerProblem(
            expression,
            keyword.textRange.shiftLeft(expression.startOffset),
            "'$keywordText' has empty body",
            fix
        )
    }

    private class RemoveElementFix(private val isIfElseBranch: Boolean = false) : LocalQuickFix {
        override fun getName() = "Remove empty construction"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            when (val expression = descriptor.psiElement) {
                is KtCallExpression -> {
                    val qualifiedExpression = expression.getQualifiedExpressionForSelector() ?: return
                    qualifiedExpression.replace(qualifiedExpression.receiverExpression)
                }
                is KtIfExpression -> {
                    if (isIfElseBranch) {
                        expression.deleteElseBranch()                        
                    } else {
                        val parentIf = expression.getStrictParentOfType<KtIfExpression>()
                        if (parentIf != null) {
                            parentIf.deleteElseBranch()
                        } else {
                            expression.delete()
                        }
                    }
                }
                else -> expression?.delete()
            }
        }
        
        private fun KtIfExpression.deleteElseBranch() {
            `else`?.delete()
            elseKeyword?.delete()            
        }
    }

    companion object {
        private val controlFlowFunctions = listOf("kotlin.also").map { FqName(it) }
    }
}
