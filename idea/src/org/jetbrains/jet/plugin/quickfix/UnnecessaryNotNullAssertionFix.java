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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author slukjanov aka Frostman
 */
public class UnnecessaryNotNullAssertionFix implements IntentionAction {
    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.unnecessary.non.null.assertion");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file instanceof JetFile && getExclExclElement(editor, file) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        final PsiElement exclExcl = getExclExclElement(editor, file);
        assert exclExcl != null;

        exclExcl.delete();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static PsiElement getExclExclElement(Editor editor, PsiFile file) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret instanceof LeafPsiElement && ((LeafPsiElement) elementAtCaret).getElementType() == JetTokens.EXCLEXCL) {
            return elementAtCaret;
        }

        return null;
    }
}
