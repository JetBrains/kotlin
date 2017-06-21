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
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isStable
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.TypeUtils

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
            expression.replaced(KtPsiFactory(lte).buildExpression {
                appendExpression(lte)
                appendFixedText("?.")
                appendExpression(rte.selectorExpression)
                appendFixedText(if (isAnd) "!= null" else "== null")
            })
            if (isNullChecksToSafeCallFixAvailable(parent as? KtBinaryExpression ?: return)) {
                applyFix(parent)
            }
        }
    }

    companion object {
        private fun isNullChecksToSafeCallFixAvailable(expression: KtBinaryExpression): Boolean {
            fun String.afterIgnoreCalls() = replace("?.", ".")

            val (lte, rte) = collectNullCheckExpressions(expression) ?: return false
            val context = expression.analyze()
            if (!lte.isChainStable(context)) return false

            val resolvedCall = rte.getResolvedCall(context) ?: return false
            val extensionReceiver = resolvedCall.extensionReceiver
            if (extensionReceiver != null && TypeUtils.isNullableType(extensionReceiver.type)) return false

            return rte.receiverExpression.text.afterIgnoreCalls() == lte.text.afterIgnoreCalls()
        }

        private fun collectNullCheckExpressions(expression: KtBinaryExpression): Triple<KtExpression, KtQualifiedExpression, Boolean>? {
            val isAnd = when (expression.operationToken) {
                KtTokens.ANDAND -> true
                KtTokens.OROR -> false
                else -> return null
            }
            val lhs = expression.left as? KtBinaryExpression ?: return null
            val rhs = expression.right as? KtBinaryExpression ?: return null
            val expectedOperation = if (isAnd) KtTokens.EXCLEQ else KtTokens.EQEQ
            val lte = lhs.getNullTestableExpression(expectedOperation) ?: return null
            val rte = rhs.getNullTestableExpression(expectedOperation) as? KtQualifiedExpression ?: return null
            return Triple(lte, rte, isAnd)
        }

        private fun KtBinaryExpression.getNullTestableExpression(expectedOperation: KtToken): KtExpression? {
            if (operationToken != expectedOperation) return null
            val lhs = left ?: return null
            val rhs = right ?: return null
            if (KtPsiUtil.isNullConstant(lhs)) return rhs
            if (KtPsiUtil.isNullConstant(rhs)) return lhs
            return null
        }

        private fun KtExpression.isChainStable(context: BindingContext): Boolean = when (this) {
            is KtReferenceExpression -> isStable(context)
            is KtQualifiedExpression -> selectorExpression?.isStable(context) == true && receiverExpression.isChainStable(context)
            else -> false
        }
    }
}
