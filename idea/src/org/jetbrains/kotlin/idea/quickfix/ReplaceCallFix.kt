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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

abstract class ReplaceCallFix(
        expression: KtExpression,
        private val receiver: String,
        private val operation: String,
        private val selector: String
) : KotlinQuickFixAction<KtExpression>(expression) {

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0$operation$1", receiver, selector)
        element.replace(newExpression)
    }
}

class ReplaceWithSafeCallFix(expression: KtExpression, receiver: String, selector: String) : ReplaceCallFix(expression, receiver, "?.", selector) {

    override fun getText() = "Replace with safe (?.) call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            return when(element) {
                is KtNameReferenceExpression -> {
                    // handling method call and property access
                    val targetElement: KtExpression = element.parent as? KtCallExpression ?: element
                    ReplaceWithSafeCallFix(targetElement, KtTokens.THIS_KEYWORD.value, targetElement.text)
                }
                else -> {
                    val qualifiedExpression = element.getParentOfType<KtDotQualifiedExpression>(strict = false) ?: return null
                    val selector = qualifiedExpression.selectorExpression?.text ?: return null
                    ReplaceWithSafeCallFix(qualifiedExpression, qualifiedExpression.receiverExpression.text, selector)
                }
            }
        }
    }
}

class ReplaceWithDotCallFix(expression: KtSafeQualifiedExpression, receiver: String, selector: String) : ReplaceCallFix(expression, receiver, ".", selector), CleanupFix {
    override fun getText() = "Replace with dot call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val qualifiedExpression = diagnostic.psiElement.getParentOfType<KtSafeQualifiedExpression>(strict = false) ?: return null
            val selector = qualifiedExpression.selectorExpression?.text ?: return null
            return ReplaceWithDotCallFix(qualifiedExpression, qualifiedExpression.receiverExpression.text, selector)
        }
    }
}
