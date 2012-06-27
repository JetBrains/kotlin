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

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author slukjanov aka Frostman
 */
@SuppressWarnings("IntentionDescriptionNotFoundInspection")
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
        if (file instanceof JetFile) {
            JetPostfixExpression postfixExpression = getExclExclPostfixExpression(editor, file);
            return (postfixExpression != null && postfixExpression.getParent() != null);
        }

        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!CodeInsightUtilBase.prepareFileForWrite(file)) {
            return;
        }

        final JetPostfixExpression postfixExpression = getExclExclPostfixExpression(editor, file);

        PsiElement parent = postfixExpression.getParent();
        if (parent != null) {
            JetExpression expression = JetPsiFactory.createExpression(project, postfixExpression.getBaseExpression().getText());
            postfixExpression.replace(expression);
        }
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

    private static JetPostfixExpression getExclExclPostfixExpression(Editor editor, PsiFile file) {
        PsiElement exclExclElement = getExclExclElement(editor, file);
        if (exclExclElement != null) {
            PsiElement parent = exclExclElement.getParent();
            if (parent instanceof JetSimpleNameExpression) {
                PsiElement operationParent = parent.getParent();
                if (operationParent instanceof JetPostfixExpression) {
                    return (JetPostfixExpression) operationParent;
                }
            }
        }

        return null;
    }
}
