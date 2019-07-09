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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.util.getFactoryForImplicitReceiverWithSubtypeOf
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*

class ImplicitThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {

        override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
            if (expression.receiverExpression != null) return

            handle(expression, expression.callableReference, ::CallableReferenceFix)
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            if (expression !is KtNameReferenceExpression) return
            if (expression.parent is KtThisExpression) return
            if (expression.parent is KtCallableReferenceExpression) return
            if (expression.isSelectorOfDotQualifiedExpression()) return
            val parent = expression.parent
            if (parent is KtCallExpression && parent.isSelectorOfDotQualifiedExpression()) return

            handle(expression, expression, ::CallFix)
        }

        private fun handle(expression: KtExpression, reference: KtReferenceExpression, fixFactory: (String) -> LocalQuickFix) {
            val context = reference.analyze()
            val scope = reference.getResolutionScope(context) ?: return

            val descriptor = reference.getCallableDescriptor() ?: return
            val receiverDescriptor = descriptor.extensionReceiverParameter
                    ?: descriptor.dispatchReceiverParameter
                    ?: return
            val receiverType = receiverDescriptor.type

            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType) ?: return
            val receiverText = if (expressionFactory.isImmediate) "this" else expressionFactory.expressionText

            val fix = fixFactory(receiverText)
            holder.registerProblem(expression, "Add explicit '$receiverText'", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fix)
        }

        private fun KtExpression.isSelectorOfDotQualifiedExpression(): Boolean {
            val parent = parent
            return parent is KtDotQualifiedExpression && parent.selectorExpression == this
        }
    }

    private class CallFix(private val receiverText: String) : LocalQuickFix {
        override fun getFamilyName() = "Add explicit '$receiverText'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtExpression ?: return
            val factory = KtPsiFactory(project)
            val call = expression.parent as? KtCallExpression ?: expression

            call.replace(factory.createExpressionByPattern("$0.$1", receiverText, call))
        }
    }

    private class CallableReferenceFix(private val receiverText: String) : LocalQuickFix {
        override fun getFamilyName() = "Add explicit '$receiverText'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtCallableReferenceExpression ?: return
            val factory = KtPsiFactory(project)
            val reference = expression.callableReference

            expression.replace(factory.createExpressionByPattern("$0::$1", receiverText, reference))
        }
    }
}