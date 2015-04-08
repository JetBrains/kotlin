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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetFile

public class KDocTypedHandler(): TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): TypedHandlerDelegate.Result =
        if (handleBeforeBracketTyped(c, editor, file)) TypedHandlerDelegate.Result.STOP else TypedHandlerDelegate.Result.CONTINUE

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): TypedHandlerDelegate.Result =
        if (handleBracketTyped(c, project, editor, file)) TypedHandlerDelegate.Result.STOP else TypedHandlerDelegate.Result.CONTINUE

    private fun handleBeforeBracketTyped(c: Char, editor: Editor, file: PsiFile): Boolean {
        if (file !is JetFile) {
            return false
        }
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            return false
        }

        val offset = editor.getCaretModel().getOffset()
        val document = editor.getDocument()
        if ((c == ']' || c == ')') && offset < document.getTextLength() && document.getCharsSequence().charAt(offset) == c) {
            PsiDocumentManager.getInstance(file.getProject()).commitDocument(document)
            val element = file.findElementAt(offset)
            // if the bracket is not part of a link, it will be part of KDOC_TEXT, not a separate RBRACKET element
            if (c == ']' &&
                element?.getNode()?.getElementType() == JetTokens.RBRACKET ||
                (offset > 0 && document.getCharsSequence().charAt(offset - 1) == '[')) {
                EditorModificationUtil.moveCaretRelatively(editor, 1)
                return true
            }
            if (c == ')' && element?.getNode()?.getElementType() == KDocTokens.MARKDOWN_INLINE_LINK) {
                EditorModificationUtil.moveCaretRelatively(editor, 1)
                return true
            }
        }
        return false
    }

    private fun handleBracketTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file !is JetFile) {
            return false
        }
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            return false
        }
        val offset = editor.getCaretModel().getOffset()
        if (offset == 0) {
            return false
        }

        if (c == '[' || c == '(') {
            val document = editor.getDocument()
            PsiDocumentManager.getInstance(project).commitDocument(document)
            val element = file.findElementAt(offset - 1)
            if (element == null ||
                element.getNode().getElementType() != KDocTokens.TEXT) {
                return false
            }
            if (c == '[')
            {
                document.insertString(offset, "]")
                return true
            }
            if (c == '(' && offset > 1 && document.getCharsSequence().charAt(offset - 2) == ']') {
                document.insertString(offset, ")")
                return true
            }
        }
        return false
    }
}
