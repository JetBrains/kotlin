package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetLanguage;

/**
 * @author max
 */
public class JetElement extends ASTWrapperPsiElement {
    public JetElement(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }
}
