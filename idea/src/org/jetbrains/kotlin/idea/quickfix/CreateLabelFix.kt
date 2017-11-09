/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

sealed class CreateLabelFix(
        expression: KtLabelReferenceExpression
) : KotlinQuickFixAction<KtLabelReferenceExpression>(expression) {
    class ForLoop(expression: KtLabelReferenceExpression) : CreateLabelFix(expression) {
        override val chooserTitle = "Select loop statement to label"

        override fun getCandidateExpressions(labelReferenceExpression: KtLabelReferenceExpression) =
                labelReferenceExpression.getContainingLoops().toList()
    }

    class ForLambda(expression: KtLabelReferenceExpression) : CreateLabelFix(expression) {
        override val chooserTitle = "Select lambda to label"

        override fun getCandidateExpressions(labelReferenceExpression: KtLabelReferenceExpression) =
                labelReferenceExpression.getContainingLambdas().toList()
    }

    override fun getFamilyName() = "Create label"

    override fun getText() = "Create label ${element?.getReferencedName() ?: ""}@"

    abstract val chooserTitle: String

    abstract fun getCandidateExpressions(labelReferenceExpression: KtLabelReferenceExpression): List<KtExpression>

    override fun startInWriteAction() = false

    private fun doCreateLabel(expression: KtLabelReferenceExpression, it: KtExpression, project: Project) {
        project.executeWriteCommand(text) {
            it.replace(KtPsiFactory(project).createExpressionByPattern("${expression.getReferencedName()}@ $0", it))
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val expression = element ?: return
        if (editor == null) return

        val containers = getCandidateExpressions(expression)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            return doCreateLabel(expression, containers.last(), project)
        }

        chooseContainerElementIfNecessary(
                containers,
                editor,
                chooserTitle,
                true,
                { it },
                {
                    doCreateLabel(expression, it, project)
                }
        )
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private fun KtLabelReferenceExpression.getContainingLoops(): Sequence<KtLoopExpression> {
            return parents
                    .takeWhile { !(it is KtDeclarationWithBody || it is KtClassBody || it is KtFile) }
                    .filterIsInstance<KtLoopExpression>()
        }

        private fun KtLabelReferenceExpression.getContainingLambdas(): Sequence<KtLambdaExpression> {
            return parents
                    .takeWhile { !(it is KtDeclarationWithBody && it !is KtFunctionLiteral || it is KtClassBody || it is KtFile) }
                    .filterIsInstance<KtLambdaExpression>()
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val labelReferenceExpression = diagnostic.psiElement as? KtLabelReferenceExpression ?: return null
            val parentExpression = (labelReferenceExpression.parent as? KtContainerNode)?.parent
            return when (parentExpression) {
                is KtBreakExpression, is KtContinueExpression -> {
                    if (labelReferenceExpression.getContainingLoops().any()) CreateLabelFix.ForLoop(labelReferenceExpression) else null
                }
                is KtReturnExpression -> {
                    if (labelReferenceExpression.getContainingLambdas().any()) CreateLabelFix.ForLambda(labelReferenceExpression) else null
                }
                else -> null
            }
        }
    }
}