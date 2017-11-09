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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
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
                    val keepBraces = expression.isElseIf() && expression.branch(constantValue) is KtBlockExpression
                    fixes += SimplifyFix(constantValue, expression.isUsedAsExpression(context), keepBraces)
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
            private val isUsedAsExpression: Boolean,
            private val keepBraces: Boolean
    ) : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Simplify expression"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val ifExpression = descriptor.psiElement.getParentOfType<KtIfExpression>(strict = true) ?: return

            val branch = ifExpression.branch(conditionValue)?.let {
                if (keepBraces) it else it.unwrapBlockOrParenthesis()
            } ?: return

            ifExpression.replaceWithBranch(branch, isUsedAsExpression, keepBraces)
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
}

private fun KtIfExpression.branch(thenBranch: Boolean) = if (thenBranch) then else `else`

private fun KtExpression.constantBooleanValue(context: BindingContext): Boolean? {
    val type = getType(context) ?: return null

    val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.toConstantValue(type)
    return constantValue?.value as? Boolean
}

fun KtExpression.replaceWithBranch(branch: KtExpression, isUsedAsExpression: Boolean, keepBraces: Boolean = false) {
    val lastExpression = when {
        branch !is KtBlockExpression -> replaced(branch)
        isUsedAsExpression -> {
            val factory = KtPsiFactory(this)
            replaced(factory.createExpressionByPattern("run $0", branch.text))
        }
        else -> {
            val firstChildSibling = branch.firstChild.nextSibling
            val lastChild = branch.lastChild
            if (firstChildSibling != lastChild) {
                if (keepBraces) {
                    parent.addAfter(branch, this)
                }
                else {
                    parent.addRangeAfter(firstChildSibling, lastChild.prevSibling, this)
                }
            }
            delete()
            null
        }
    }

    val caretModel = branch.findExistingEditor()?.caretModel
    caretModel?.moveToOffset(lastExpression?.startOffset ?: return)
}