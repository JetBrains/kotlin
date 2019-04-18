/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

class NullableBooleanElvisInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        binaryExpressionVisitor(fun(expression) {
            if (expression.operationToken != KtTokens.ELVIS) return
            val lhs = expression.left ?: return
            val rhs = expression.right ?: return
            if (!KtPsiUtil.isBooleanConstant(rhs)) return
            val lhsType = lhs.analyze(BodyResolveMode.PARTIAL).getType(lhs) ?: return
            if (TypeUtils.isNullableType(lhsType) && lhsType.isBooleanOrNullableBoolean()) {
                val parentIfOrWhile = PsiTreeUtil.getParentOfType(
                        expression, KtIfExpression::class.java, KtWhileExpressionBase::class.java)
                val condition = when (parentIfOrWhile) {
                    is KtIfExpression -> parentIfOrWhile.condition
                    is KtWhileExpressionBase -> parentIfOrWhile.condition
                    else -> null
                }
                val (highlightType, verb) = if (condition != null && condition in expression.parentsWithSelf)
                    GENERIC_ERROR_OR_WARNING to "should"
                else
                    INFORMATION to "can"

                holder.registerProblemWithoutOfflineInformation(
                        expression,
                        "Equality check $verb be used instead of elvis for nullable boolean check",
                        isOnTheFly,
                        highlightType,
                        ReplaceWithEqualityCheckFix()
                )
            }
        })

    private class ReplaceWithEqualityCheckFix : LocalQuickFix {
        override fun getName() = "Replace with equality check"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement as? KtBinaryExpression ?: return
            if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) return
            if (element.operationToken != KtTokens.ELVIS) return
            val constPart = element.right as? KtConstantExpression ?: return
            val exprPart = element.left ?: return

            val constValue = when {
                KtPsiUtil.isTrueConstant(constPart) -> true
                KtPsiUtil.isFalseConstant(constPart) -> false
                else -> return
            }
            val equalityCheckExpression = element.replaced(KtPsiFactory(constPart).buildExpression {
                appendExpression(exprPart)
                appendFixedText(if (constValue) " != false" else " == true")
            })
            val prefixExpression = equalityCheckExpression.getParentOfType<KtPrefixExpression>(strict = true) ?: return
            val simplifier = SimplifyNegatedBinaryExpressionInspection()
            if (simplifier.isApplicable(prefixExpression)) {
                simplifier.applyTo(prefixExpression.operationReference)
            }
        }
    }
}

