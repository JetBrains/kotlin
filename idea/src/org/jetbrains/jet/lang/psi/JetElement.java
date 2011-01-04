package org.jetbrains.jet.lang.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElementVisitor;
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

    @Override
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            accept((JetVisitor) visitor);
        }
        else {
            visitor.visitElement(this);
        }
    }

    public void accept(JetVisitor visitor) {
        visitor.visitJetElement(this);
    }
}
