package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;

/**
 * @author svtk
 */
public interface JetModifierListOwner extends PsiElement {
    @Nullable
    JetModifierList getModifierList();

    boolean hasModifier(JetToken modifier);
}
