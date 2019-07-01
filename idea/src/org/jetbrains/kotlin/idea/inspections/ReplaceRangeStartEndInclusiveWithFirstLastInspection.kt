/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.dotQualifiedExpressionVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class ReplaceRangeStartEndInclusiveWithFirstLastInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return dotQualifiedExpressionVisitor(fun(expression: KtDotQualifiedExpression) {
            val selectorExpression = expression.selectorExpression ?: return
            if (selectorExpression.text != "start" && selectorExpression.text != "endInclusive") return

            val resolvedCall = expression.resolveToCall() ?: return
            val containing = resolvedCall.resultingDescriptor.containingDeclaration as? ClassDescriptor ?: return
            if (!containing.isRange()) return

            if (selectorExpression.text == "start") {
                holder.registerProblem(
                    expression,
                    "Could be replaced with unboxed `first`",
                    ReplaceIntRangeStartWithFirstQuickFix()
                )
            } else if (selectorExpression.text == "endInclusive") {
                holder.registerProblem(
                    expression,
                    "Could be replaced with unboxed `last`",
                    ReplaceIntRangeEndInclusiveWithLastQuickFix()
                )
            }
        })
    }
}

private val rangeTypes = setOf(
    "kotlin.ranges.IntRange",
    "kotlin.ranges.CharRange",
    "kotlin.ranges.LongRange",
    "kotlin.ranges.UIntRange",
    "kotlin.ranges.ULongRange"
)

private fun ClassDescriptor.isRange(): Boolean {
    return rangeTypes.any { this.fqNameUnsafe.asString() == it }
}

class ReplaceIntRangeStartWithFirstQuickFix : LocalQuickFix {
    override fun getName() = "Replace 'start' with 'first'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        val selector = element.selectorExpression ?: return
        selector.replace(KtPsiFactory(element).createExpression("first"))
    }
}

class ReplaceIntRangeEndInclusiveWithLastQuickFix : LocalQuickFix {
    override fun getName() = "Replace 'endInclusive' with 'last'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtDotQualifiedExpression
        val selector = element.selectorExpression ?: return
        selector.replace(KtPsiFactory(element).createExpression("last"))
    }
}