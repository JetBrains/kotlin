/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;

public class KotlinTypedHandler extends TypedHandlerDelegate {

    private boolean jetLTTyped;

    @Override
    public Result beforeCharTyped(char c, Project project, Editor editor, PsiFile file, FileType fileType) {
        jetLTTyped = '<' == c &&
                     file instanceof JetFile && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET &&
                     JetLtGtTypingUtils.shouldAutoCloseAngleBracket(editor.getCaretModel().getOffset(), editor);

        if ('>' == c) {
            if (file instanceof JetFile && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
                if (JetLtGtTypingUtils.handleJetGTInsert(editor)) {
                    return Result.STOP;
                }
            }
        }

        return super.beforeCharTyped(c, project, editor, file, fileType);
    }

    @Override
    public Result charTyped(char c, Project project, Editor editor, @NotNull PsiFile file) {
        if (jetLTTyped) {
            jetLTTyped = false;
            JetLtGtTypingUtils.handleJetAutoCloseLT(editor);
            return Result.STOP;
        }

        if (!(file instanceof JetFile) || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            return Result.CONTINUE;
        }
        if (c == '{') {
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            int offset = editor.getCaretModel().getOffset();
            PsiElement previousElement = file.findElementAt(offset - 1);
            if (previousElement instanceof LeafPsiElement
                    && ((LeafPsiElement) previousElement).getElementType() == JetTokens.LONG_TEMPLATE_ENTRY_START) {
                editor.getDocument().insertString(offset, "}");
            }
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
