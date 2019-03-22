/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RedundantSetterInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return propertyAccessorVisitor { accessor ->
            val rangeInElement = accessor.namePlaceholder.textRange?.shiftRight(-accessor.startOffset) ?: return@propertyAccessorVisitor
            if (accessor.isRedundantSetter()) {
                holder.registerProblem(
                    accessor,
                    "Redundant setter",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    rangeInElement,
                    RemoveRedundantSetterFix()
                )
            }
        }
    }
}

private fun KtPropertyAccessor.isRedundantSetter(): Boolean {
    if (!isSetter) return false
    if (annotationEntries.isNotEmpty()) return false
    if (hasLowerVisibilityThanProperty()) return false
    val expression = bodyExpression ?: return true
    if (expression is KtBlockExpression) {
        val statement = expression.statements.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val parameter = valueParameters.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val binaryExpression = statement as? KtBinaryExpression ?: return false
        return binaryExpression.operationToken == KtTokens.EQ
                && binaryExpression.left?.isFieldText() == true
                && binaryExpression.right?.mainReference?.resolve() == parameter
    }
    return false
}

private fun KtPropertyAccessor.hasLowerVisibilityThanProperty(): Boolean {
    val p = property
    return when {
        p.hasModifier(KtTokens.PRIVATE_KEYWORD) ->
            false
        p.hasModifier(KtTokens.PROTECTED_KEYWORD) ->
            hasModifier(KtTokens.PRIVATE_KEYWORD)
        p.hasModifier(KtTokens.INTERNAL_KEYWORD) ->
            hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.PROTECTED_KEYWORD)
        else ->
            hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.PROTECTED_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)
    }
}

private fun KtExpression.isFieldText(): Boolean = this.textMatches("field")

private class RemoveRedundantSetterFix : LocalQuickFix {
    override fun getName() = "Remove redundant setter"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val accessor = descriptor.psiElement as? KtPropertyAccessor ?: return
        accessor.delete()
    }
}