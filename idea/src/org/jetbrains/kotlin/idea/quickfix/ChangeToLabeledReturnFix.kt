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
import org.jetbrains.kotlin.idea.completion.returnExpressionItems
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern

class ChangeToLabeledReturnFix(element: KtReturnExpression, val labeledReturn: String) : KotlinQuickFixAction<KtReturnExpression>(element) {
    override fun getFamilyName() = "Change to return with label"
    override fun getText() = "Change to '$labeledReturn'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val returnExpression = element ?: return
        val factory = KtPsiFactory(project)
        val returnedExpression = returnExpression.returnedExpression
        val newExpression = if (returnedExpression == null) factory.createExpression(labeledReturn) else factory.createExpressionByPattern("$0 $1", labeledReturn, returnedExpression.text)
        returnExpression.replace(newExpression)
    }

    companion object : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val expression = diagnostic.psiElement as? KtReturnExpression ?: return emptyList()
            return returnExpressionItems(expression.analyze(), expression).map {
                ChangeToLabeledReturnFix(expression, labeledReturn = it.lookupString)
            }
        }
    }
}