package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetArgument extends JetElement {
    public JetArgument(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(JetVisitor visitor) {
        visitor.visitArgument(this);
    }
}
