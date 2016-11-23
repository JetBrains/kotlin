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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression.KotlinParenthesesSurrounder
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class UnsupportedAsyncFix(val psiElement: PsiElement): KotlinQuickFixAction<PsiElement>(psiElement), CleanupFix {
    override fun getFamilyName(): String = "Migrate unsupported async syntax"
    override fun getText(): String  = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element
        if (element is KtBinaryExpression) {
            if (element.operationToken != KtTokens.IDENTIFIER) {
                // async+ {}
                element.addBefore(KtPsiFactory(element).createWhiteSpace(), element.operationReference)
            }
            else if (element.right != null) {
                // foo async {}
                KotlinParenthesesSurrounder.surroundWithParentheses(element.right!!)
            }
        }

        if (element is KtCallExpression) {
            val ktExpression = element.calleeExpression ?: return

            // Add after "async" reference in call
            element.addAfter(KtPsiFactory(element).createCallArguments("()"), ktExpression)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.psiElement.text != "async" ||
                !Errors.UNSUPPORTED.cast(diagnostic).a.startsWith("async block/lambda")) return null

            // Identifier -> Expression -> Call (normal call) or Identifier -> Operation Reference -> Binary Expression (for infix usage)
            val grand = diagnostic.psiElement.parent.parent
            if (grand is KtBinaryExpression || grand is KtCallExpression) {
                return UnsupportedAsyncFix(grand)
            }

            return null
        }
    }
}
