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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.findLabelAndCall
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class ChangeToLabeledReturnFix(
        element: KtReturnExpression, val labeledReturn: String
) : KotlinQuickFixAction<KtReturnExpression>(element) {

    override fun getFamilyName() = "Change to return with label"
    override fun getText() = "Change to '$labeledReturn'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val returnExpression = element ?: return
        val factory = KtPsiFactory(project)
        val returnedExpression = returnExpression.returnedExpression
        val newExpression = if (returnedExpression == null)
            factory.createExpression(labeledReturn)
        else
            factory.createExpressionByPattern("$0 $1", labeledReturn, returnedExpression)
        returnExpression.replace(newExpression)
    }

    companion object : KotlinIntentionActionsFactory() {
        private fun findAccessibleLabels(bindingContext: BindingContext, position: KtReturnExpression): List<Name> {
            val result = mutableListOf<Name>()
            for (parent in position.parentsWithSelf) {
                if (parent is KtFunctionLiteral) {
                    val (label, call) = parent.findLabelAndCall()
                    if (label != null) {
                        result.add(label)
                    }

                    // check if the current function literal is inlined and stop processing outer declarations if it's not
                    val callee = call?.calleeExpression as? KtReferenceExpression ?: break
                    if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break
                }
            }
            return result
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val expression = diagnostic.psiElement as? KtReturnExpression ?: return emptyList()
            return findAccessibleLabels(expression.analyze(), expression).map {
                ChangeToLabeledReturnFix(expression, labeledReturn = "return@${it.render()}")
            }
        }
    }
}