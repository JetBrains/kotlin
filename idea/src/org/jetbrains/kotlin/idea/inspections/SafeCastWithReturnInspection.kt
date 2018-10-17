/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SafeCastWithReturnInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        binaryWithTypeRHSExpressionVisitor(fun(expression) {
            if (expression.right == null) return
            if (expression.operationReference.getReferencedName() != "as?") return

            val parent = expression.getStrictParentOfType<KtBinaryExpression>() ?: return
            if (KtPsiUtil.deparenthesize(parent.left) != expression) return
            if (parent.operationReference.getReferencedName() != "?:") return
            if (KtPsiUtil.deparenthesize(parent.right) !is KtReturnExpression) return

            val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            if (context[BindingContext.USED_AS_EXPRESSION, parent] == true) return
            if (context.diagnostics.forElement(expression.operationReference).any { it.factory == Errors.CAST_NEVER_SUCCEEDS }) return

            holder.registerProblem(
                parent,
                "Should be replaced with 'if' type check",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceWithIfFix()
            )
        })
}

private class ReplaceWithIfFix : LocalQuickFix {
    override fun getName() = "Replace with 'if' type check"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val elvisExpression = descriptor.psiElement as? KtBinaryExpression ?: return
        val returnExpression = KtPsiUtil.deparenthesize(elvisExpression.right) ?: return
        val safeCastExpression = KtPsiUtil.deparenthesize(elvisExpression.left) as? KtBinaryExpressionWithTypeRHS ?: return
        val typeReference = safeCastExpression.right ?: return
        elvisExpression.replace(
            KtPsiFactory(elvisExpression).createExpressionByPattern(
                "if ($0 !is $1) $2",
                safeCastExpression.left,
                typeReference,
                returnExpression
            )
        )
    }
}