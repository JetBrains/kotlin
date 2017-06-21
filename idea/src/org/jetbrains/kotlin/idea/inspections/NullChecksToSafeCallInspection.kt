/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.PsiEquivalenceUtil.getFilteredChildren
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class NullChecksToSafeCallInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (isNullChecksToSafeCallFixAvailable(expression)) {
                        holder.registerProblem(expression,
                                               "Null-checks replaceable with safe-calls",
                                               ProblemHighlightType.WEAK_WARNING,
                                               NullChecksToSafeCallCheckFix())
                    }
                }
            }

    private class NullChecksToSafeCallCheckFix : LocalQuickFix {
        override fun getName() = "Replace chained null-checks with safe-calls"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            applyFix(descriptor.psiElement as? KtBinaryExpression ?: return)
        }

        private fun applyFix(expression: KtBinaryExpression) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(expression)) return
            val (lte, rte, isAnd) = collectNullCheckExpressions(expression) ?: return
            val parent = expression.parent
            val element = getDuplicates(lte, rte) ?: return
            expression.replaced(KtPsiFactory(lte).buildExpression {
                appendExpression(lte)
                appendFixedText("?" + rte.text.removePrefix(element.text))
                appendFixedText(if (isAnd) "!= null" else "== null")
            })
            if (isNullChecksToSafeCallFixAvailable(parent as? KtBinaryExpression ?: return)) {
                applyFix(parent)
            }
        }
    }
}

private fun collectNullCheckExpressions(expression: KtBinaryExpression): Triple<KtExpression, KtQualifiedExpression, Boolean>? {
    val isAnd = expression.operationToken == KtTokens.ANDAND
    if (!isAnd && expression.operationToken != KtTokens.OROR) return null
    val lhs = expression.left as? KtBinaryExpression ?: return null
    val rhs = expression.right as? KtBinaryExpression ?: return null
    val expectedOperation = if (isAnd) KtTokens.EXCLEQ else KtTokens.EQEQ
    val lte = lhs.getNullTestableExpression(expectedOperation) ?: return null
    val rte = rhs.getNullTestableExpression(expectedOperation) as? KtQualifiedExpression ?: return null
    return Triple(lte, rte, isAnd)
}

private fun isNullChecksToSafeCallFixAvailable(expression: KtBinaryExpression): Boolean {
    val (lte, rte) = collectNullCheckExpressions(expression) ?: return false
    return getDuplicates(lte, rte)?.parent as? KtDotQualifiedExpression != null
}

private fun KtBinaryExpression.getNullTestableExpression(expectedOperation: KtToken): KtExpression? {
    if (operationToken != expectedOperation) return null
    val lhs = left ?: return null
    val rhs = right ?: return null
    if (KtPsiUtil.isNullConstant(lhs)) return rhs
    if (KtPsiUtil.isNullConstant(rhs)) return lhs
    return null
}

private fun getDuplicates(element: KtExpression, scope: PsiElement): PsiElement? {
    val children = getFilteredChildren(scope, null, true)
    for (child in children) {
        if (element.isEqTo(child)) {
            return child
        }
        val v = getDuplicates(element, child)
        if (v != null) {
            return v
        }
    }
    return null
}

private fun KtExpression.isEqTo(other: PsiElement?): Boolean {
    other ?: return false
    return when (this) {
        is KtReferenceExpression -> other is KtReferenceExpression && mainReference.resolve() == other.mainReference.resolve()
        is KtQualifiedExpression -> other is KtQualifiedExpression && receiverExpression.isEqTo(other.receiverExpression)
                                    && selectorExpression?.isEqTo(other.selectorExpression) ?: false
        else -> false
    }
}