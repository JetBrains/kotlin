/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*

class ChangeToFunctionInvocationFix(element: KtExpression) : KotlinQuickFixAction<KtExpression>(element) {
    override fun getFamilyName() = "Change to function invocation"

    override fun getText() = familyName

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(element)
        val nextLiteralStringEntry = element.parent.nextSibling as? KtLiteralStringTemplateEntry
        val nextText = nextLiteralStringEntry?.text
        if (nextText != null && nextText.startsWith("(") && nextText.contains(")")) {
            val parentheses = nextText.takeWhile { it != ')' } + ")"
            val newNextText = nextText.removePrefix(parentheses)
            if (newNextText.isNotEmpty()) {
                nextLiteralStringEntry.replace(psiFactory.createLiteralStringTemplateEntry(newNextText))
            } else {
                nextLiteralStringEntry.delete()
            }
            element.replace(KtPsiFactory(file).createExpressionByPattern("$0$1", element, parentheses))
        } else {
            element.replace(KtPsiFactory(file).createExpressionByPattern("$0()", element))
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            return ChangeToFunctionInvocationFix(expression)
        }
    }
}
