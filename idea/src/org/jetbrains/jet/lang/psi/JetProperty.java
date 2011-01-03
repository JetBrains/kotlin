package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetProperty extends JetNamedDeclaration {
    public JetProperty(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitProperty(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
