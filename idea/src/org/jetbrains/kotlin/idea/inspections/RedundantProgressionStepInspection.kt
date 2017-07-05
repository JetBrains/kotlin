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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class RedundantProgressionStepInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (isStepOneCall(expression)) {
                    holder.registerProblem(expression,
                                           "Iteration step is redundant. Recommended to remove it.",
                                           ProblemHighlightType.WEAK_WARNING,
                                           TextRange(expression.operationReference.startOffsetInParent, expression.endOffset - expression.startOffset),
                                           EliminateStepOneFix())
                }
            }
        }
    }


    class EliminateStepOneFix : LocalQuickFix {
        override fun getName() = "Eliminate redundant iteration step size"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val expression = descriptor.psiElement as? KtBinaryExpression ?: return
            if (!isStepOneCall(expression)) return
            expression.replaced(expression.left!!)
        }
    }

    companion object {
        private val STEP_FUNCTION_FQ_NAME = "kotlin.ranges.step"

        fun isStepOneCall(expression: KtBinaryExpression): Boolean {
            val left = expression.left as? KtBinaryExpression ?: return false
            if (left.operationToken != KtTokens.RANGE) return false
            if (expression.operationReference.text != "step") return false
            val right = expression.right as? KtConstantExpression ?: return false
            val rightConst = right.firstChild as? LeafPsiElement ?: return false
            if (rightConst.elementType != KtTokens.INTEGER_LITERAL) return false

            if (rightConst.text !in setOf("1", "1L")) return false

            val context = expression.analyze()

            val call = context[BindingContext.CALL, expression.operationReference]

            val resolvedCall = call.getResolvedCall(context) ?: return false
            val fqNameString = resolvedCall.resultingDescriptor.importableFqName?.asString() ?: return false
            if (fqNameString != STEP_FUNCTION_FQ_NAME) return false
            return true
        }
    }
}