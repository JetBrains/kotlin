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

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.refactoring.BaseRefactoringIntentionAction
import org.jetbrains.kotlin.idea.conversion.copy.range

class MoveDeclarationsIntentionAction(
        private val processor: MoveDeclarationsProcessor,
        private val bounds: RangeMarker,
        private val modificationCount: Long
) : BaseRefactoringIntentionAction(), HintAction {

    private val isSingleDeclaration = processor.pastedDeclarations.size == 1

    override fun startInWriteAction() = false

    override fun getText() = "Update usages to reflect declaration${if (isSingleDeclaration) "s" else ""} move"
    override fun getFamilyName() = "Update usages on declarations cut/paste"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        return PsiModificationTracker.SERVICE.getInstance(processor.project).modificationCount == modificationCount
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        processor.performRefactoring()
    }

    override fun showHint(editor: Editor): Boolean {
        val range = bounds.range ?: return false
        if (editor.caretModel.offset != range.endOffset) return false

        if (PsiModificationTracker.SERVICE.getInstance(processor.project).modificationCount != modificationCount) return false

        val hintText = "$text? ${KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS))}"
        HintManager.getInstance().showQuestionHint(editor, hintText, range.endOffset, range.endOffset) {
            processor.performRefactoring()
            true
        }

        return true
    }
}