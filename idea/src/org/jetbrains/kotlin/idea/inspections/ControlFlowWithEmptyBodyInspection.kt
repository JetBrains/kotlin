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
                val branch = expression.`else`
                val hasEmptyBranches = (branch as? KtIfExpression)?.hasEmptyBranches() ?: branch.isEmptyBody()
                val fix = when {
                    !hasEmptyBranches -> null
                    !expression.condition.hasNoSideEffect() -> when {
                        expression.parentIf() != null -> null
                        expression.condition.sideEffects().isEmpty() -> null
                        else -> RemoveElementFix(extractSideEffects = true)
                    }
                    else -> RemoveElementFix()
                }
                holder.registerProblem(expression, expression.ifKeyword, fix)
            }
            val elseKeyword = expression.elseKeyword
            if (elseKeyword != null && expression.`else`.isEmptyBody()) {
                holder.registerProblem(expression, elseKeyword, RemoveElementFix(isIfElseBranch = true))
            }
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            if (expression.entries.isNotEmpty() || expression.hasComment()) return
            val subjectExpression = expression.subjectExpression
            val fix = when {
                subjectExpression.hasNoSideEffect() -> RemoveElementFix()
                subjectExpression.sideEffects().isNotEmpty() -> RemoveElementFix(extractSideEffects = true)
                else -> null
            }
            holder.registerProblem(expression, expression.whenKeyword, fix)
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
            val body = when (val argument = expression.valueArguments.singleOrNull()?.getArgumentExpression()) {
                is KtLambdaExpression -> argument.bodyExpression
                is KtNamedFunction -> argument.bodyBlockExpression
                else -> return
            }
            if (body.isEmptyBody()) {
                holder.registerProblem(expression, callee, RemoveElementFix())
            }
        }
    }
    
    private fun KtExpression?.hasNoSideEffect(): Boolean {
        if (this == null) return true
        return this is KtSimpleNameExpression 
                || this is KtConstantExpression 
                || (this is KtBinaryExpression && this.left.hasNoSideEffect() && this.right.hasNoSideEffect())
                || (this is KtProperty && this.initializer.hasNoSideEffect())
    }

    private fun KtIfExpression.hasEmptyBranches(): Boolean {
        if (!condition.hasNoSideEffect()) return false
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

    private fun ProblemsHolder.registerProblem(expression: KtExpression, keyword: PsiElement, fix: LocalQuickFix? = null) {
        val keywordText = if (expression is KtDoWhileExpression) "do while" else keyword.text
        registerProblem(
            expression,
            keyword.textRange.shiftLeft(expression.startOffset),
            "'$keywordText' has empty body",
            fix
        )
    }

    private class RemoveElementFix(
        private val extractSideEffects: Boolean = false,
        private val isIfElseBranch: Boolean = false
    ) : LocalQuickFix {
        override fun getName() = if (extractSideEffects) "Extract side effects" else "Remove empty construction"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement

            if (extractSideEffects) {
                val sideEffects = when (expression) {
                    is KtIfExpression -> expression.condition.sideEffects()
                    is KtWhenExpression -> expression.subjectExpression.sideEffects()
                    else -> emptyList()
                }
                val parent = expression.parent
                val psiFactory = KtPsiFactory(expression)
                sideEffects.forEachIndexed { index, sideEffect ->
                    if (index > 0) {
                        parent.addBefore(psiFactory.createNewLine(), expression)
                    }
                    parent.addBefore(sideEffect, expression)
                }
            }
            
            when (expression) {
                is KtCallExpression -> {
                    val qualifiedExpression = expression.getQualifiedExpressionForSelector()
                    if (qualifiedExpression != null) {
                        qualifiedExpression.replace(qualifiedExpression.receiverExpression)
                    } else {
                        expression.delete()
                    }
                }
                is KtIfExpression -> {
                    if (isIfElseBranch) {
                        expression.deleteElseBranch()                        
                    } else {
                        val parentIf = expression.parentIf()
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

private fun KtIfExpression.parentIf(): KtIfExpression? {
    return getStrictParentOfType<KtIfExpression>()?.takeIf { it.`else` == this }
}

private fun KtExpression?.sideEffects(): List<KtExpression> {
    return when (this) {
        is KtCallExpression, is KtQualifiedExpression -> listOf(this)
        is KtBinaryExpression -> listOfNotNull(left, right)
        is KtProperty -> listOfNotNull(initializer)
        else -> emptyList()
    }
}
