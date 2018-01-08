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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KotlinRawStringTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE) {
            return Result.CONTINUE
        }
        if (file !is KtFile) {
            return Result.CONTINUE
        }
        if (c != '"') {
            return Result.CONTINUE
        }
        // A quote is typed after 2 other quotes
        val offset = editor.caretModel.offset
        val psiElement = file.findElementAt(offset) ?: return Result.CONTINUE
        if (PsiTreeUtil.getParentOfType(psiElement, KtStringTemplateExpression::class.java) != null) {
            return Result.CONTINUE
        }

        val text = editor.document.text
        if (offset >= 2)
            if (text[offset - 1] == '"')
                if (text[offset - 2] == '"') {
                    editor.document.insertString(offset, "\"\"\"\"")
                    editor.caretModel.currentCaret.moveToOffset(offset + 1)
                    return Result.STOP
                }

        return Result.CONTINUE
    }
}