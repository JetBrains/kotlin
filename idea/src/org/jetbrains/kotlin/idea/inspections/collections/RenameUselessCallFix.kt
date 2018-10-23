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

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType

class RenameUselessCallFix(val newName: String) : LocalQuickFix {
    override fun getName() = "Change call to '$newName'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        (descriptor.psiElement as? KtQualifiedExpression)?.let {
            val factory = KtPsiFactory(it)
            val selectorCallExpression = it.selectorExpression as? KtCallExpression
            val calleeExpression = selectorCallExpression?.calleeExpression ?: return
            calleeExpression.replaced(factory.createExpression(newName))
            selectorCallExpression.renameGivenReturnLabels(factory, calleeExpression.text, newName)
        }
    }

    private fun KtCallExpression.renameGivenReturnLabels(factory: KtPsiFactory, labelName: String, newName: String) {
        val lambdaExpression = lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return
        val bodyExpression = lambdaExpression.bodyExpression ?: return

        bodyExpression.forEachDescendantOfType<KtReturnExpression> {
            if (it.getLabelName() != labelName) return@forEachDescendantOfType

            it.replaced(
                factory.createExpressionByPattern(
                    "return@$0 $1",
                    newName,
                    it.returnedExpression ?: ""
                )
            )
        }
    }
}