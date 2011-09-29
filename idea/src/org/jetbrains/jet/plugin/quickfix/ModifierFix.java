package org.jetbrains.jet.plugin.quickfix;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public abstract class ModifierFix extends JetIntentionAction<JetModifierListOwner> {
    protected final JetKeywordToken modifier;

    protected ModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier) {
        super(element);
        this.modifier = modifier;
    }

    @NotNull
    protected String getElementName() {
        String name = null;
        if (element instanceof PsiNameIdentifierOwner) {
            PsiElement nameIdentifier = ((PsiNameIdentifierOwner) element).getNameIdentifier();
            if (nameIdentifier != null) {
                name = nameIdentifier.getText();
            }
        }
        else if (element instanceof JetPropertyAccessor) {
            name = ((JetPropertyAccessor) element).getNamePlaceholder().getText();
        }
        if (name == null) {
            name = element.getText();
        }
        return "'" + name + "'";
    }
}
