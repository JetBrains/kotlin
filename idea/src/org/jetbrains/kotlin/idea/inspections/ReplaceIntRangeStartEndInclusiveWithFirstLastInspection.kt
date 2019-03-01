/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class ReplaceIntRangeStartEndInclusiveWithFirstLastInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor { expression ->
            val fName: FqName? = expression.GetFunctionName()

            if (fName != null && fName.isStart()) {
                holder.registerProblem(
                    expression,
                    "Could be replaced with `first`",
                    ReplaceIntRangeStartWithFirstQuickFix()
                )
            } else if (fName != null && fName.isEndInclusive()) {
                holder.registerProblem(
                    expression,
                    "Could be replaced with `last`",
                    ReplaceIntRangeEndInclusiveWithLastQuickFix()
                )
            }
        }
    }
}

private fun KtDotQualifiedExpression.GetFunctionName(): FqName? {
    val context = this.analyze()
    return this.getResolvedCall(context)?.resultingDescriptor?.fqNameOrNull()
}

private fun FqName.isStart(): Boolean {
    return this == FqName("kotlin.ranges.IntRange.start")
}

private fun FqName.isEndInclusive(): Boolean {
    return this == FqName("kotlin.ranges.IntRange.endInclusive")
}

class ReplaceIntRangeStartWithFirstQuickFix : LocalQuickFix {
    override fun getName() = "Replace 'start' with 'first'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        element.selectorExpression!!.replace(KtPsiFactory(element).createExpression("first"))
    }
}

class ReplaceIntRangeEndInclusiveWithLastQuickFix : LocalQuickFix {
    override fun getName() = "Replace 'endInclusive' with 'last'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        element.selectorExpression!!.replace(KtPsiFactory(element).createExpression("last"))
    }
}