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

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class MoveWhenElseBranchFix(element: KtWhenExpression) : KotlinQuickFixAction<KtWhenExpression>(element) {
    override fun getFamilyName() = "Move else branch to the end"

    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && KtPsiUtil.checkWhenExpressionHasSingleElse(element)
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val entries = element.entries
        val lastEntry = entries.lastOrNull() ?: return
        val elseEntry = entries.singleOrNull { it.isElse } ?: return

        val cursorOffset = editor!!.caretModel.offset - elseEntry.textOffset

        val insertedBranch = element.addAfter(elseEntry, lastEntry) as KtWhenEntry
        elseEntry.delete()
        val insertedWhenEntry = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(insertedBranch)

        editor.caretModel.moveToOffset(insertedWhenEntry.textOffset + cursorOffset)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): MoveWhenElseBranchFix? {
            val whenExpression = diagnostic.psiElement.getNonStrictParentOfType<KtWhenExpression>() ?: return null
            return MoveWhenElseBranchFix(whenExpression)
        }
    }
}
