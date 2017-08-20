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
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConstantConditionIfInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)

                val condition = expression.condition ?: return

                val context = condition.analyze(BodyResolveMode.PARTIAL)
                val constantValue = condition.constantBooleanValue(context) ?: return

                val fixes = mutableListOf<LocalQuickFix>()

                if (expression.branch(constantValue) != null) {
                    fixes += SimplifyFix(constantValue, expression.isUsedAsExpression(context))
                }

                if (!constantValue && expression.`else` == null) {
                    fixes += RemoveFix()
                }

                holder.registerProblem(condition,
                                       "Condition is always '$constantValue'",
                                       *fixes.toTypedArray())
            }
        }
    }

    private class SimplifyFix(
            private val conditionValue: Boolean,
            private val isUsedAsExpression: Boolean
    ) : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Simplify expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val ifExpression = descriptor.psiElement.getParentOfType<KtIfExpression>(strict = true) ?: return
            val caretModel = ifExpression.findExistingEditor()?.caretModel

            val branch = ifExpression.branch(conditionValue)?.unwrapBlockOrParenthesis() ?: return

            val lastExpression = when {
                branch !is KtBlockExpression -> ifExpression.replaced(branch)
                isUsedAsExpression -> {
                    val factory = KtPsiFactory(ifExpression)
                    ifExpression.replaced(factory.createExpressionByPattern("run $0", branch.text))
                }
                else -> {
                    val firstChild = branch.firstChild.nextSibling

                    if (firstChild == branch.lastChild) {
                        ifExpression.delete()
                    }
                    else {
                        val lastChild = branch.lastChild.prevSibling
                        val parent = ifExpression.parent
                        parent.addRangeAfter(firstChild, lastChild, ifExpression)
                        ifExpression.delete()
                    }

                    null
                }
            }

            caretModel?.moveToOffset(lastExpression?.startOffset ?: return)
        }
    }

    private class RemoveFix : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Delete expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val ifExpression = descriptor.psiElement.getParentOfType<KtIfExpression>(strict = true) ?: return
            ifExpression.delete()
        }
    }

    private companion object {
        private fun KtIfExpression.branch(thenBranch: Boolean) = if (thenBranch) then else `else`

        private fun KtExpression.constantBooleanValue(context: BindingContext): Boolean? {
            val type = getType(context) ?: return null

            val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.toConstantValue(type)
            return constantValue?.value as? Boolean
        }
    }
}