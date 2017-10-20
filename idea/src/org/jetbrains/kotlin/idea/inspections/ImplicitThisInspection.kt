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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.util.getFactoryForImplicitReceiverWithSubtypeOf
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext

class ImplicitThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitExpression(expression: KtExpression) {
            if (expression !is KtNameReferenceExpression) return
            if (expression.isSelectorOfDotQualifiedExpression()) return
            val parent = expression.parent
            if (parent is KtCallExpression && parent.isSelectorOfDotQualifiedExpression()) return

            val referenceExpression = expression as? KtNameReferenceExpression
                                      ?: expression.getChildOfType()
                                      ?: return

            val context = expression.analyzeFully()
            val scope = expression.getResolutionScope(context) ?: return

            val descriptor = context[BindingContext.REFERENCE_TARGET, referenceExpression] as? CallableDescriptor ?: return
            val receiverDescriptor = descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter ?: return
            val receiverType = receiverDescriptor.type

            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType) ?: return
            val receiverText = if (expressionFactory.isImmediate) "this" else expressionFactory.expressionText

            holder.registerProblem(expression, "Add explicit '$receiverText'", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, Fix(receiverText))
        }

        private fun KtExpression.isSelectorOfDotQualifiedExpression(): Boolean {
            val parent = parent
            return parent is KtDotQualifiedExpression && parent.selectorExpression == this
        }
    }

    private class Fix(private val receiverText: String) : LocalQuickFix {
        override fun getFamilyName() = "Add explicit '$receiverText'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtExpression ?: return
            val factory = KtPsiFactory(project)
            val call = expression.parent as? KtCallExpression ?: expression

            call.replace(factory.createExpressionByPattern("$0.$1", receiverText, call.text))
        }
    }
}