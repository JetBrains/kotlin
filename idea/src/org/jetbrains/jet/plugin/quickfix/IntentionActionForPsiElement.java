package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author svtk
*/
public abstract class IntentionActionForPsiElement<T extends PsiElement> implements IntentionAction {
    protected @NotNull T element;

    public IntentionActionForPsiElement(@NotNull T element) {
        this.element = element;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
