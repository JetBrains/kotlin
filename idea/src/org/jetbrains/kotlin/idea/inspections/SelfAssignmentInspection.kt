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

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver

class SelfAssignmentInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            private fun KtExpression.asNameReferenceExpression(): KtNameReferenceExpression? = when (this) {
                is KtNameReferenceExpression ->
                    this
                is KtDotQualifiedExpression ->
                    (selectorExpression as? KtNameReferenceExpression)?.takeIf { receiverExpression is KtThisExpression }
                else ->
                    null
            }

            private fun KtExpression.receiverDeclarationDescriptor(
                    resolvedCall: ResolvedCall<out CallableDescriptor>,
                    context: BindingContext
            ): DeclarationDescriptor? {
                val thisExpression = (this as? KtDotQualifiedExpression)?.receiverExpression as? KtThisExpression
                if (thisExpression != null) {
                    return thisExpression.getResolvedCall(context)?.resultingDescriptor?.containingDeclaration
                }
                val implicitReceiver = with (resolvedCall) { dispatchReceiver ?: extensionReceiver } as? ImplicitReceiver
                return implicitReceiver?.declarationDescriptor
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)

                if (expression.operationToken != KtTokens.EQ) return
                val left = expression.left
                val leftRefExpr = left?.asNameReferenceExpression() ?: return
                val right = expression.right
                val rightRefExpr = right?.asNameReferenceExpression() ?: return
                // To omit analyzing too much
                if (leftRefExpr.text != rightRefExpr.text) return

                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val leftResolvedCall = left.getResolvedCall(context)
                val leftCallee = leftResolvedCall?.resultingDescriptor as? VariableDescriptor ?: return
                val rightResolvedCall = right.getResolvedCall(context)
                val rightCallee = rightResolvedCall?.resultingDescriptor as? VariableDescriptor ?: return
                if (leftCallee != rightCallee) return

                if (!rightCallee.isVar) return
                if (rightCallee is PropertyDescriptor) {
                    if (rightCallee.isOverridable) return
                    if (rightCallee.accessors.any { !it.isDefault }) return
                }

                if (left.receiverDeclarationDescriptor(leftResolvedCall, context) !=
                    right.receiverDeclarationDescriptor(rightResolvedCall, context)) {
                    return
                }

                holder.registerProblem(right,
                                       "Variable '${rightCallee.name}' is assigned to itself",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       RemoveSelfAssignmentFix())
            }
        }
    }
}

private class RemoveSelfAssignmentFix : LocalQuickFix {
    override fun getName() = "Remove self assignment"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val right = descriptor.psiElement as? KtExpression ?: return
        right.parent.delete()
    }
}
