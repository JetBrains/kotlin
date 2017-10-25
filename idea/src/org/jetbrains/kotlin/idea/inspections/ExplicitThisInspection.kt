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
import com.intellij.codeInspection.ProblemHighlightType.LIKE_UNUSED_SYMBOL
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.LexicalScope

class ExplicitThisInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            val thisExpression = expression.receiverExpression as? KtThisExpression ?: return

            val context = expression.analyzeFully()
            val scope = expression.getResolutionScope(context) ?: return

            val selectorExpression = expression.selectorExpression as? KtReferenceExpression ?: return
            val referenceExpression = selectorExpression as? KtNameReferenceExpression
                                      ?: selectorExpression.getChildOfType()
                                      ?: return

            val scopeFunction = when (selectorExpression) {
                is KtNameReferenceExpression -> LexicalScope::getAllAccessibleVariables
                is KtCallExpression -> LexicalScope::getAllAccessibleFunctions
                else -> return
            }

            //we avoid overload-related problems by enforcing that there is only one candidate
            val candidates = scopeFunction(scope, referenceExpression.getReferencedNameAsName())
            if (candidates.size != 1) return


            val receiverType = context[BindingContext.EXPRESSION_TYPE_INFO, thisExpression]?.type ?: return
            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType) ?: return

            val label = thisExpression.getLabelName() ?: ""
            if (!expressionFactory.matchesLabel(label)) return

            holder.registerProblem(thisExpression, "Redundant explicit this", LIKE_UNUSED_SYMBOL, Fix(thisExpression.text))
        }

        private fun ReceiverExpressionFactory.matchesLabel(label: String): Boolean {
            val implicitLabel = expressionText.substringAfter("@", "")
            return label == implicitLabel || (label == "" && isImmediate)
        }
    }

    private class Fix(private val text: String) : LocalQuickFix {
        override fun getFamilyName(): String = "Remove redundant '$text'"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val thisExpression = descriptor.psiElement as? KtThisExpression ?: return
            val parent = thisExpression.parent as? KtDotQualifiedExpression ?: return
            parent.replace(parent.selectorExpression ?: return)
        }
    }
}