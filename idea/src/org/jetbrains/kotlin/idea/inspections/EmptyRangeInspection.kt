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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getArguments
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class EmptyRangeInspection : AbstractPrimitiveRangeToInspection() {
    override fun visitRangeToExpression(expression: KtExpression, holder: ProblemsHolder) {
        val (left, right) = expression.getArguments() ?: return

        val context = expression.analyze(BodyResolveMode.PARTIAL)
        val startValue = left?.longValueOrNull(context) ?: return
        val endValue = right?.longValueOrNull(context) ?: return

        if (startValue <= endValue) return

        holder.registerProblem(
            expression,
            "This range is empty. Did you mean to use 'downTo'?",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceWithDownToFix()
        )
    }

    class ReplaceWithDownToFix : LocalQuickFix {
        override fun getName() = "Replace with 'downTo'"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as? KtExpression ?: return
            val (left, right) = element.getArguments() ?: return
            if (left == null || right == null) return

            element.replace(KtPsiFactory(element).createExpressionByPattern("$0 downTo $1", left, right))
        }
    }

    private fun KtExpression.longValueOrNull(context: BindingContext): Long? {
        return (constantValueOrNull(context)?.value as? Number)?.toLong()
    }
}