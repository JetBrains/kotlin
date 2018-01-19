/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.previousStatement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isUnit

class RedundantUnitExpressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return referenceExpressionVisitor(fun(expression) {
            if (expression.isRedundantUnit()) {
                holder.registerProblem(
                    expression,
                    "Redundant 'Unit'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantUnitFix()
                )
            }
        })
    }
}

private fun KtReferenceExpression.isRedundantUnit(): Boolean {
    if (!isUnitLiteral()) return false
    val parent = this.parent ?: return false
    if (parent is KtReturnExpression) return true
    if (parent is KtBlockExpression) {
        // Do not report just 'Unit' in function literals (return@label Unit is OK even in literals)
        if (parent.getParentOfType<KtFunctionLiteral>(strict = true) != null) return false

        if (this == parent.lastBlockStatementOrThis()) {
            val prev = this.previousStatement() ?: return true
            if (prev.isUnitLiteral()) return true
            if (prev.getResolvedCall(analyze())?.resultingDescriptor?.returnType?.isUnit() == true) return true
            if (prev is KtDeclaration) {
                return if (prev is KtFunction)
                    parent.parent?.parent?.let { it is KtIfExpression || it is KtWhenExpression } != true
                else
                    true
            }
            return false
        }

        return true
    }
    return false
}

private fun KtExpression.isUnitLiteral(): Boolean =
    KotlinBuiltIns.FQ_NAMES.unit.shortName() == (this as? KtNameReferenceExpression)?.getReferencedNameAsName()

private class RemoveRedundantUnitFix : LocalQuickFix {
    override fun getName() = "Remove redundant 'Unit'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtReferenceExpression)?.delete()
    }
}
