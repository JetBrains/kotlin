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

package org.jetbrains.kotlin.idea.editor;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.psi.JetFile;

public class KotlinBackspaceHandler extends BackspaceHandlerDelegate {
    private boolean deleteGt;

    @Override
    public void beforeCharDeleted(char c, PsiFile file, Editor editor) {
        int offset = editor.getCaretModel().getOffset() - 1;
        deleteGt = c =='<' && file instanceof JetFile && JetLtGtTypingUtils.shouldAutoCloseAngleBracket(offset, editor);
    }

    @Override
    public boolean charDeleted(char c, PsiFile file, Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        CharSequence chars = editor.getDocument().getCharsSequence();
        if (editor.getDocument().getTextLength() <= offset) return false; //virtual space after end of file

        char c1 = chars.charAt(offset);
        if (c == '<' && deleteGt) {
            if (c1 == '>') {
                JetLtGtTypingUtils.handleJetLTDeletion(editor, offset);
            }
            return true;
        }

        return false;
    }
}
