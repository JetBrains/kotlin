package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetSafeQualifiedExpression extends JetQualifiedExpression {
    public JetSafeQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitSafeQualifiedExpression(this);
    }
}
