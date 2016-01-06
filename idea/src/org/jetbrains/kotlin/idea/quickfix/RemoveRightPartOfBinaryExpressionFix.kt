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
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveRightPartOfBinaryExpressionFix<T : KtExpression>(
        element: T,
        private val message: String
) : KotlinQuickFixAction<T>(element), CleanupFix {
    override fun getFamilyName() = "Remove right part of a binary expression"

    override fun getText(): String = message

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        invoke()
    }

    fun invoke(): KtExpression {
        val newExpression = when (element) {
            is KtBinaryExpression -> element.replace((element.copy() as KtBinaryExpression).left!!) as KtExpression
            is KtBinaryExpressionWithTypeRHS -> element.replace((element.copy() as KtBinaryExpressionWithTypeRHS).left) as KtExpression
            else -> throw IncorrectOperationException("Unexpected element: " + element.getElementTextWithContext())
        }

        val parent = newExpression.parent
        if (parent is KtParenthesizedExpression && KtPsiUtil.areParenthesesUseless(parent)) {
            return parent.replace(newExpression) as KtExpression
        }
        return newExpression
    }

    object RemoveCastFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtBinaryExpressionWithTypeRHS>? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtBinaryExpressionWithTypeRHS>() ?: return null
            return RemoveRightPartOfBinaryExpressionFix(expression, "Remove cast")
        }
    }

    object RemoveElvisOperatorFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtBinaryExpression>? {
            val expression = diagnostic.psiElement as? KtBinaryExpression ?: return null
            return RemoveRightPartOfBinaryExpressionFix(expression, "Remove elvis operator")
        }
    }
}

