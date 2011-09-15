package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
* @author svtk
*/
public abstract class IntentionActionForPsiElement<T extends PsiElement> implements IntentionAction {
    protected @NotNull T element;

    public IntentionActionForPsiElement(@NotNull T element) {
        this.element = element;
    }
}
