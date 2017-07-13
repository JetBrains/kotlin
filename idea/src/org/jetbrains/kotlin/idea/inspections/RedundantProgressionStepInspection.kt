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
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

class RedundantProgressionStepInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (isStepOneCall(expression.left, expression.right, expression.operationReference)) {
                    holder.registerProblem(expression,
                                           "Iteration step is redundant. Remove it.",
                                           ProblemHighlightType.WEAK_WARNING,
                                           TextRange(expression.operationReference.startOffsetInParent, expression.endOffset - expression.startOffset),
                                           EliminateStepOneFix())
                }
            }

            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {

            }
        }
    }


    class EliminateStepOneFix : LocalQuickFix {
        override fun getName() = "Eliminate redundant iteration step size"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val expression = descriptor.psiElement as? KtBinaryExpression ?: return
            if (!isStepOneCall(expression.left, expression.right, expression.operationReference)) return
            expression.replaced(expression.left!!)
        }
    }

    companion object {
        private val STEP_FUNCTION_FQ_NAME = "kotlin.ranges.step"

        fun isStepOneCall(leftParenthesized: KtExpression?, rightParenthesized: KtExpression?, reference: KtSimpleNameExpression): Boolean {
            val left = KtPsiUtil.deparenthesize(leftParenthesized) as? KtBinaryExpression ?: return false
            val right = KtPsiUtil.deparenthesize(rightParenthesized) as? KtConstantExpression ?: return false
            if (left.operationToken != KtTokens.RANGE) return false
            if (reference.text != "step") return false

            val rightConst = right.firstChild as? LeafPsiElement ?: return false
            if (rightConst.elementType != KtTokens.INTEGER_LITERAL) return false

            val context = (left.parent as KtElement).analyze()

            val constant = ConstantExpressionEvaluator.getConstant(right, right.analyze()) ?: return false
            val builtIns = left.containingKtFile.findModuleDescriptor().builtIns

            if (constant.getValue(builtIns.longType) != 1L &&
                constant.getValue(builtIns.intType) != 1) return false

            val call = context[BindingContext.CALL, reference]

            val resolvedCall = call.getResolvedCall(context) ?: return false
            val fqNameString = resolvedCall.resultingDescriptor.importableFqName?.asString() ?: return false
            if (fqNameString != STEP_FUNCTION_FQ_NAME) return false
            return true
        }
    }
}