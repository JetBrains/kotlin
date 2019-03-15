/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class RedundantGetterInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return propertyAccessorVisitor { accessor ->
            if (accessor.isRedundantGetter()) {
                holder.registerProblem(
                    accessor,
                    "Redundant getter",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    RemoveRedundantGetterFix()
                )
            }
        }
    }
}

private fun KtPropertyAccessor.isRedundantGetter(): Boolean {
    if (!isGetter) return false
    if (hasModifier(KtTokens.EXTERNAL_KEYWORD)) return false
    if (annotationEntries.isNotEmpty()) return false
    val expression = bodyExpression ?: return true
    if (expression is KtNameReferenceExpression) {
        return expression.isFieldText()
    }
    if (expression is KtBlockExpression) {
        val statement = expression.statements.takeIf { it.size == 1 }?.firstOrNull() ?: return false
        val returnExpression = statement as? KtReturnExpression ?: return false
        return returnExpression.returnedExpression?.isFieldText() == true
    }
    return false
}

private fun KtExpression.isFieldText(): Boolean = this.textMatches("field")

private class RemoveRedundantGetterFix : LocalQuickFix {
    override fun getName() = "Remove redundant getter"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val accessor = descriptor.psiElement as? KtPropertyAccessor ?: return
        val property = accessor.property

        val accessorTypeReference = accessor.returnTypeReference
        if (accessorTypeReference != null && property.typeReference == null && property.initializer == null) {
            property.typeReference = accessorTypeReference
        }

        accessor.delete()
    }
}