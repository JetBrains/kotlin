package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetDecomposer extends JetNamedDeclaration {
    public JetDecomposer(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitDecomposer(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
