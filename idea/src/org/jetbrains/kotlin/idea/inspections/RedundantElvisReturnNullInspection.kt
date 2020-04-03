/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RedundantElvisReturnNullInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return returnExpressionVisitor(fun(returnExpression: KtReturnExpression) {
            if ((returnExpression.returnedExpression?.deparenthesize() as? KtConstantExpression)?.text != KtTokens.NULL_KEYWORD.value) return

            val binaryExpression = returnExpression.getStrictParentOfType<KtBinaryExpression>()?.takeIf {
                it == it.getStrictParentOfType<KtReturnExpression>()?.returnedExpression?.deparenthesize()
            } ?: return
            val right = binaryExpression.right?.deparenthesize()?.takeIf { it == returnExpression } ?: return
            if (binaryExpression.operationToken == KtTokens.ELSE_KEYWORD) return
            if (binaryExpression.left?.resolveToCall()?.resultingDescriptor?.returnType?.isMarkedNullable != true) return

            holder.registerProblem(
                binaryExpression,
                KotlinBundle.message("inspection.redundant.elvis.return.null.descriptor"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                TextRange(binaryExpression.operationReference.startOffset, right.endOffset).shiftLeft(binaryExpression.startOffset),
                RemoveRedundantElvisReturnNull()
            )
        })
    }

    private fun KtExpression.deparenthesize() = KtPsiUtil.deparenthesize(this)

    private class RemoveRedundantElvisReturnNull : LocalQuickFix {
        override fun getName() = KotlinBundle.message("remove.redundant.elvis.return.null.text")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binaryExpression = descriptor.psiElement as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            binaryExpression.replace(left)
        }
    }
}
