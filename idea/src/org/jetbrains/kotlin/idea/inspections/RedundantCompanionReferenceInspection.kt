/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils

class RedundantCompanionReferenceInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {

            val descriptor = (expression.mainReference.resolve() as? KtObjectDeclaration)?.descriptor ?: return
            if (!DescriptorUtils.isCompanionObject(descriptor)) return

            val parent = expression.getStrictParentOfType<KtDotQualifiedExpression>() ?: return
            if (expression == parent.receiverExpression && expression.text == descriptor.containingDeclaration?.name?.asString()) return
            if (expression == parent.selectorExpression && parent.parent !is KtDotQualifiedExpression) return

            holder.registerProblem(
                expression,
                "Redundant Companion reference",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveRedundantCompanionReferenceFix()
            )
        })
    }
}

private class RemoveRedundantCompanionReferenceFix : LocalQuickFix {
    override fun getName() = "Remove redundant Companion reference"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expression = descriptor.psiElement as? KtReferenceExpression ?: return
        val parent = expression.getStrictParentOfType<KtDotQualifiedExpression>() ?: return
        val selector = parent.selectorExpression ?: return
        val receiver = parent.receiverExpression
        if (expression == receiver) parent.replace(selector) else parent.replace(receiver)
    }
}