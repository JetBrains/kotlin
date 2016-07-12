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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class WrapWithSafeLetCallFix(
        expression: KtExpression,
        val nullableExpression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression) {

    override fun getFamilyName() = text

    override fun getText() = "Wrap with '?.let { ... }' call"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val factory = KtPsiFactory(element)
        val nullableText = nullableExpression.text
        nullableExpression.replace(factory.createExpression("it"))
        val wrapped = factory.createExpressionByPattern("$0?.let { $1 }", nullableText, element.text)
        element.replace(wrapped)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val expression = element.getParentOfType<KtExpression>(true) ?: return null

            val parent = element.parent
            val nullableExpression = (parent as? KtCallExpression)?.calleeExpression ?: return null

            return WrapWithSafeLetCallFix(expression, nullableExpression)
        }
    }
}