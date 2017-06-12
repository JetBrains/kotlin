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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue

abstract class ReplaceCallFix(
        expression: KtQualifiedExpression,
        private val operation: String
) : KotlinQuickFixAction<KtQualifiedExpression>(expression) {

    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && element.selectorExpression != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newExpression = KtPsiFactory(element).createExpressionByPattern("$0$operation$1",
                                                                            element.receiverExpression, element.selectorExpression!!)
        element.replace(newExpression)
    }
}

class ReplaceImplicitReceiverCallFix(expression: KtExpression) : KotlinQuickFixAction<KtExpression>(expression) {
    override fun getFamilyName() = text

    override fun getText() = "Replace with safe (this?.) call"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newExpression = KtPsiFactory(element).createExpressionByPattern("this?.$0", element)
        element.replace(newExpression)
    }
}

class ReplaceWithSafeCallFix(expression: KtDotQualifiedExpression): ReplaceCallFix(expression, "?.") {

    override fun getText() = "Replace with safe (?.) call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val psiElement = diagnostic.psiElement
            val qualifiedExpression = psiElement.parent as? KtDotQualifiedExpression
            if (qualifiedExpression != null) {
                return ReplaceWithSafeCallFix(qualifiedExpression)
            } else {
                psiElement as? KtNameReferenceExpression ?: return null
                if (psiElement.getResolvedCall(psiElement.analyze())?.getImplicitReceiverValue() != null) {
                    val expressionToReplace: KtExpression = psiElement.parent as? KtCallExpression ?: psiElement
                    return ReplaceImplicitReceiverCallFix(expressionToReplace)
                }
                return null
            }
        }
    }
}

class ReplaceWithDotCallFix(expression: KtSafeQualifiedExpression): ReplaceCallFix(expression, "."), CleanupFix {
    override fun getText() = "Replace with dot call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val qualifiedExpression = diagnostic.psiElement.getParentOfType<KtSafeQualifiedExpression>(strict = false) ?: return null
            return ReplaceWithDotCallFix(qualifiedExpression)
        }
    }
}
