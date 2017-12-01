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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile

class KotlinStringTemplateBackspaceHandler : BackspaceHandlerDelegate() {
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        if (c != '{' || file !is KtFile || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return

        val offset = editor?.caretModel?.offset ?: return

        val highlighter = (editor as EditorEx).highlighter
        val iterator = highlighter.createIterator(offset)
        if (iterator.tokenType != KtTokens.LONG_TEMPLATE_ENTRY_END) return
        iterator.retreat()
        if (iterator.tokenType != KtTokens.LONG_TEMPLATE_ENTRY_START) return
        editor.document.deleteString(offset, offset + 1)
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        return false
    }
}