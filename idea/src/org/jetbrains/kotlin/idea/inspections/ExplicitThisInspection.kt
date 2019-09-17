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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class ExplicitThisInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitExpression(expression: KtExpression) {
            val thisExpression = expression.thisAsReceiverOrNull() ?: return
            if (hasExplicitThis(expression)) {
                holder.registerProblem(
                    thisExpression,
                    "Redundant explicit this",
                    LIKE_UNUSED_SYMBOL,
                    ExplicitThisExpressionFix(thisExpression.text)
                )
            }
        }
    }

    companion object {
        fun KtExpression.thisAsReceiverOrNull() = when (this) {
            is KtCallableReferenceExpression -> receiverExpression as? KtThisExpression
            is KtDotQualifiedExpression -> receiverExpression as? KtThisExpression
            else -> null
        }

        fun hasExplicitThis(expression: KtExpression): Boolean {
            val thisExpression = expression.thisAsReceiverOrNull() ?: return false
            val reference = when (expression) {
                is KtCallableReferenceExpression -> expression.callableReference
                is KtDotQualifiedExpression -> expression.selectorExpression as? KtReferenceExpression
                else -> null
            } ?: return false
            val context = expression.analyze()
            val scope = expression.getResolutionScope(context) ?: return false

            val referenceExpression = reference as? KtNameReferenceExpression ?: reference.getChildOfType() ?: return false
            val receiverType = context[BindingContext.EXPRESSION_TYPE_INFO, thisExpression]?.type ?: return false

            //we avoid overload-related problems by enforcing that there is only one candidate
            val name = referenceExpression.getReferencedNameAsName()
            val candidates = if (reference is KtCallExpression
                || (expression is KtCallableReferenceExpression && reference.mainReference.resolve() is KtFunction)
            ) {
                scope.getAllAccessibleFunctions(name) +
                        scope.getAllAccessibleVariables(name).filter { it is LocalVariableDescriptor && it.canInvoke() }
            } else {
                scope.getAllAccessibleVariables(name)
            }
            if (referenceExpression.getCallableDescriptor() is SyntheticJavaPropertyDescriptor) {
                if (candidates.map { it.containingDeclaration }.distinct().size != 1) return false
            } else {
                val candidate = candidates.singleOrNull() ?: return false
                val extensionType = candidate.extensionReceiverParameter?.type
                if (extensionType != null && extensionType != receiverType && receiverType.isSubtypeOf(extensionType)) return false
            }

            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType) ?: return false

            val label = thisExpression.getLabelName() ?: ""
            if (!expressionFactory.matchesLabel(label)) return false
            return true
        }

        private fun VariableDescriptor.canInvoke(): Boolean {
            val declarationDescriptor = this.type.constructor.declarationDescriptor as? LazyClassDescriptor ?: return false
            return declarationDescriptor.declaredCallableMembers.any { (it as? FunctionDescriptor)?.isOperator == true }
        }

        private fun ReceiverExpressionFactory.matchesLabel(label: String): Boolean {
            val implicitLabel = expressionText.substringAfter("@", "")
            return label == implicitLabel || (label == "" && isImmediate)
        }
    }
}

class ExplicitThisExpressionFix(private val text: String) : LocalQuickFix {
    override fun getFamilyName(): String = "Remove redundant '$text'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val thisExpression = descriptor.psiElement as? KtThisExpression ?: return
        removeExplicitThisExpression(thisExpression)
    }

    companion object {
        fun removeExplicitThisExpression(thisExpression: KtThisExpression) {
            when (val parent = thisExpression.parent) {
                is KtDotQualifiedExpression -> parent.replace(parent.selectorExpression ?: return)
                is KtCallableReferenceExpression -> thisExpression.delete()
            }
        }
    }
}

