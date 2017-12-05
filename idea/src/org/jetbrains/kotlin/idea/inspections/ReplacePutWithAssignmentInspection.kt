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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf

class ReplacePutWithAssignmentInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                if (isActiveFor(expression)) {
                    holder.registerProblem(
                            expression.callExpression!!.calleeExpression!!,
                            "map.put() can be converted to assignment",
                            ProblemHighlightType.WEAK_WARNING,
                            ReplacePutWithAssignmentQuickfix()
                    )
                }
            }
        }
    }

    companion object {
        private val compatibleNames = setOf("put")

        fun isActiveFor(expression: KtDotQualifiedExpression): Boolean {
            val callExpression = expression.callExpression
            if (callExpression?.valueArguments?.size != 2) return false

            val calleeExpression = callExpression.calleeExpression as? KtNameReferenceExpression ?: return false
            if (calleeExpression.getReferencedName() !in compatibleNames) return false

            val context = expression.analyze()
            if (expression.isUsedAsExpression(context)) return false
            val resolvedCall = expression.getResolvedCall(context)
            val receiverType = resolvedCall?.getExplicitReceiverValue()?.type ?: return false
            val receiverClass = receiverType.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            return receiverClass.isSubclassOf(DefaultBuiltIns.Instance.mutableMap)
        }

        fun simplify(expression: KtDotQualifiedExpression) {
            val valueArguments = expression.callExpression?.valueArguments ?: return
            expression.replace(KtPsiFactory(expression).createExpressionByPattern("$0[$1] = $2",
                                                                                  expression.receiverExpression,
                                                                                  valueArguments[0]?.getArgumentExpression() ?: return,
                                                                                  valueArguments[1]?.getArgumentExpression() ?: return))
        }
    }

    class ReplacePutWithAssignmentQuickfix : LocalQuickFix {
        override fun getName() = "Convert put to assignment"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            simplify(descriptor.psiElement.parent.parent as? KtDotQualifiedExpression ?: return)
        }
    }
}