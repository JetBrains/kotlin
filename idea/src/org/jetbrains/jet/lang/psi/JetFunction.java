package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetFunction extends JetTypeParameterListOwner {
    public JetFunction(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitFunction(this);
    }
}
