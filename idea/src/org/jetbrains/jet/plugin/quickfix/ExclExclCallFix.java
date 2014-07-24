/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPostfixExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

@SuppressWarnings("IntentionDescriptionNotFoundInspection")
public class ExclExclCallFix implements IntentionAction {

    private final boolean isRemove;

    private ExclExclCallFix(boolean remove) {
        isRemove = remove;
    }

    public static ExclExclCallFix removeExclExclCall() {
        return new ExclExclCallFix(true);
    }

    public static ExclExclCallFix introduceExclExclCall() {
        return new ExclExclCallFix(false);
    }

    @NotNull
    @Override
    public String getText() {
        return isRemove ? JetBundle.message("remove.unnecessary.non.null.assertion") : JetBundle.message("introduce.non.null.assertion");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (file instanceof JetFile) {
            if (!isRemove) {
                return isAvailableForIntroduce(editor, file);
            }
            else {
                return isAvailableForRemove(editor, file);
            }
        }

        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }

        JetPsiFactory psiFactory = JetPsiFactory(project);
        if (!isRemove) {
            JetExpression modifiedExpression = getExpressionForIntroduceCall(editor, file);
            JetExpression exclExclExpression = psiFactory.createExpression(modifiedExpression.getText() + "!!");
            modifiedExpression.replace(exclExclExpression);
        }
        else {
            JetPostfixExpression postfixExpression = getExclExclPostfixExpression(editor, file);
            JetExpression expression = psiFactory.createExpression(postfixExpression.getBaseExpression().getText());
            postfixExpression.replace(expression);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static boolean isAvailableForIntroduce(Editor editor, PsiFile file) {
        return getExpressionForIntroduceCall(editor, file) != null;
    }

    private static boolean isAvailableForRemove(Editor editor, PsiFile file) {
        return getExclExclPostfixExpression(editor, file) != null;
    }

    private static PsiElement getExclExclElement(Editor editor, PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (isExclExclLeaf(elementAtCaret)) {
            return elementAtCaret;
        }

        if (elementAtCaret != null) {
            // Case when caret is placed right after !!
            PsiElement prevLeaf = PsiTreeUtil.prevLeaf(elementAtCaret);
            if (isExclExclLeaf(prevLeaf)) {
                return prevLeaf;
            }
        }

        return null;
    }

    private static boolean isExclExclLeaf(@Nullable PsiElement element) {
        return (element instanceof LeafPsiElement) && ((LeafPsiElement) element).getElementType() == JetTokens.EXCLEXCL;
    }

    private static JetExpression getExpressionForIntroduceCall(Editor editor, PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAtCaret != null) {
            JetExpression expression = getExpressionForIntroduceCall(elementAtCaret);
            if (expression != null) {
                return expression;
            }

            // Maybe caret is after error element
            expression = getExpressionForIntroduceCall(PsiTreeUtil.prevLeaf(elementAtCaret));
            if (expression != null) {
                return expression;
            }
        }

        return null;
    }

    private static JetExpression getExpressionForIntroduceCall(PsiElement problemElement) {
        if (problemElement instanceof LeafPsiElement && ((LeafPsiElement) problemElement).getElementType() == JetTokens.DOT) {
            PsiElement sibling = problemElement.getPrevSibling();
            if (sibling instanceof JetExpression) {
                return (JetExpression) sibling;
            }
        }

        return null;
    }

    private static JetPostfixExpression getExclExclPostfixExpression(Editor editor, PsiFile file) {
        PsiElement exclExclElement = getExclExclElement(editor, file);

        if (exclExclElement != null) {
            PsiElement parent = exclExclElement.getParent();
            if (parent != null) {
                PsiElement operationParent = parent.getParent();
                if (operationParent instanceof JetPostfixExpression) {
                    return (JetPostfixExpression) operationParent;
                }
            }
        }

        return null;
    }
}
