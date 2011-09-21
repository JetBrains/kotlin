package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public abstract class ModifierFix extends IntentionActionForPsiElement<JetModifierListOwner> {
    protected final JetKeywordToken modifier;

    protected ModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier) {
        super(element);
        this.modifier = modifier;
    }

    @NotNull
    protected String getElementName() {
        if (element instanceof PsiNameIdentifierOwner) {
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
            if (nameIdentifier != null) {
                return "'" + nameIdentifier.getText() + "'";
            }
        }
        return "'" + element.getText() + "'";
    }
}
