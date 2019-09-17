/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

fun KtPropertyAccessor.isRedundantSetter(): Boolean {
    if (!isSetter) return false
    val expression = bodyExpression ?: return canBeCompletelyDeleted()
    if (expression is KtBlockExpression) {
        val statement = expression.statements.singleOrNull() ?: return false
        val parameter = valueParameters.singleOrNull() ?: return false
        val binaryExpression = statement as? KtBinaryExpression ?: return false
        return binaryExpression.operationToken == KtTokens.EQ
                && binaryExpression.left?.isBackingFieldReferenceTo(property) == true
                && binaryExpression.right?.mainReference?.resolve() == parameter
    }
    return false
}


class RemoveRedundantSetterFix : LocalQuickFix {
    override fun getName() = "Remove redundant setter"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val accessor = descriptor.psiElement as? KtPropertyAccessor ?: return
        removeRedundantSetter(accessor)
    }

    companion object {
        fun removeRedundantSetter(setter: KtPropertyAccessor) {
            if (setter.canBeCompletelyDeleted()) {
                setter.delete()
            } else {
                setter.deleteBody()
            }
        }
    }
}