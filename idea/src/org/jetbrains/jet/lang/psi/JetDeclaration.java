package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public abstract class JetDeclaration extends JetElement {
    public JetDeclaration(@NotNull ASTNode node) {
        super(node);
    }
}
