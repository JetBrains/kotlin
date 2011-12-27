package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author ignatov
 */
@SuppressWarnings("IntentionDescriptionNotFoundInspection")
public class ReplaceDotCallWithSafeCall implements IntentionAction {
    public ReplaceDotCallWithSafeCall() {
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("replace.with.safe.call");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("replace.with.safe.call");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (file instanceof JetFile) {
            return getDotCallExpression(editor, (JetFile) file) != null;
        }
        return false;
    }

    private static JetDotQualifiedExpression getDotCallExpression(@NotNull Editor editor, @NotNull JetFile file) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        return PsiTreeUtil.getParentOfType(elementAtCaret, JetDotQualifiedExpression.class);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetDotQualifiedExpression dotCallExpression = getDotCallExpression(editor, (JetFile) file);
        assert dotCallExpression != null;

        JetExpression selector = dotCallExpression.getSelectorExpression();
        if (selector != null) {
            JetSafeQualifiedExpression newElement = (JetSafeQualifiedExpression) JetPsiFactory.createExpression(
                    project, dotCallExpression.getReceiverExpression().getText() + "?." + selector.getText());

            dotCallExpression.replace(newElement);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
