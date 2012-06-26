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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 * @author slukjanov aka Frostman
 */
public class ReplaceCallFix implements IntentionAction {
    private final boolean safe;
    private final boolean fromDot;

    private ReplaceCallFix(boolean safe, boolean fromDot) {
        this.safe = safe;
        this.fromDot = fromDot;
    }

    /**
     * @return quickfix for replacing dot call with safe (?.) call
     */
    public static ReplaceCallFix toSafeCall() {
        return new ReplaceCallFix(true, true);
    }

    /**
     * @return quickfix for replacing dot call with non-null asserted (!!.) call
     */
    public static ReplaceCallFix toNonNullAssertedCall() {
        return new ReplaceCallFix(false, true);
    }

    /**
     * @return quickfix for replacing unnecessary safe (?.) call with dot call
     */
    public static ReplaceCallFix toDotCallFromSafeCall() {
        return new ReplaceCallFix(true, false);
    }

    /**
     * @return quickfix for replacing unnecessary non-null asserted (!!.) call with dot call
     */
    public static ReplaceCallFix toDotCallFromNonNullAssertedCall() {
        return new ReplaceCallFix(false, false);
    }

    @NotNull
    @Override
    public String getText() {
        return fromDot
               ? (safe ? JetBundle.message("replace.with.safe.call") : JetBundle.message("replace.with.nna.call"))
               : JetBundle.message("replace.with.dot.call");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (file instanceof JetFile) {
            return getCallExpression(editor, (JetFile) file) != null;
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetQualifiedExpression callExpression = getCallExpression(editor, (JetFile) file);
        assert callExpression != null;

        JetExpression selector = callExpression.getSelectorExpression();
        if (selector != null) {
            if (!fromDot && !safe) {
                final PsiElement elementAtCaret = getElementAtCaret(editor, file);
                if (elementAtCaret instanceof LeafPsiElement) {
                    final LeafPsiElement leafElement = (LeafPsiElement) elementAtCaret;
                    PsiElement exclExclElement = null;
                    if (leafElement.getElementType() == JetTokens.EXCLEXCL) {
                        exclExclElement = leafElement;
                    }
                    else if (leafElement.getElementType() == JetTokens.DOT) {
                        PsiElement prevSibling = leafElement.getPrevSibling();
                        if (prevSibling != null) {
                            exclExclElement = prevSibling.getLastChild();
                        }
                    }
                    if (exclExclElement != null) {
                        exclExclElement.delete();
                    }
                }
            }

            JetQualifiedExpression newElement = (JetQualifiedExpression) JetPsiFactory.createExpression(
                    project,
                    callExpression.getReceiverExpression().getText() + (fromDot ? (safe ? "?." : "!!.") : ".") + selector.getText());

            callExpression.replace(newElement);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private JetQualifiedExpression getCallExpression(@NotNull Editor editor, @NotNull JetFile file) {
        final PsiElement elementAtCaret = getElementAtCaret(editor, file);
        return PsiTreeUtil
                .getParentOfType(elementAtCaret, fromDot || !safe ? JetDotQualifiedExpression.class : JetSafeQualifiedExpression.class);
    }

    private static PsiElement getElementAtCaret(Editor editor, PsiFile file) {
        return file.findElementAt(editor.getCaretModel().getOffset());
    }
}
