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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow

class ReplaceNegatedIsEmptyWithIsNotEmptyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return simpleNameExpressionVisitor { simpleNameExpression ->
            if (simpleNameExpression.isEmptyNegation()) {
                holder.registerProblem(
                    simpleNameExpression,
                    "Replace negated 'isEmpty' with 'isNotEmpty'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceNegatedIsEmptyWithIsNotEmptyQuickFix()
                )
            }
        }
    }
}

class ReplaceNegatedIsEmptyWithIsNotEmptyQuickFix : LocalQuickFix {
    override fun getName() = "Replace negated 'isEmpty' with 'isNotEmpty'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtSimpleNameExpression ?: return
        val callExpression = (element.parent as? KtCallExpression) ?: return
        val qualifiedExpression = (callExpression.parent as? KtDotQualifiedExpression) ?: return
        val prefixExpression = qualifiedExpression.getWrappingPrefixExpressionIfAny() ?: return

        prefixExpression.replaced(
            KtPsiFactory(element).createExpressionByPattern(
                "$0.isNotEmpty()",
                qualifiedExpression.receiverExpression
            )
        )
    }
}

private fun KtSimpleNameExpression.isEmptyNegation(): Boolean {
    val callExpression = (parent as? KtCallExpression) ?: return false
    val qualifiedExpression = (callExpression.parent as? KtDotQualifiedExpression) ?: return false
    val prefixExpression = qualifiedExpression.getWrappingPrefixExpressionIfAny() ?: return false
    if (prefixExpression.operationToken != KtTokens.EXCL) return false
    return transformations.any { callExpression.isCalling(FqName(it)) }
}

private fun PsiElement.getWrappingPrefixExpressionIfAny() =
    (getLastParentOfTypeInRow<KtParenthesizedExpression>() ?: this).parent as? KtPrefixExpression

private val transformations = listOf(
    "java.util.ArrayList.isEmpty",
    "java.util.HashMap.isEmpty",
    "java.util.HashSet.isEmpty",
    "java.util.LinkedHashMap.isEmpty",
    "java.util.LinkedHashSet.isEmpty",
    "kotlin.collections.isEmpty",
    "kotlin.collections.List.isEmpty",
    "kotlin.collections.Set.isEmpty",
    "kotlin.collections.Map.isEmpty",
    "kotlin.collections.MutableList.isEmpty",
    "kotlin.collections.MutableSet.isEmpty",
    "kotlin.collections.MutableMap.isEmpty"
)