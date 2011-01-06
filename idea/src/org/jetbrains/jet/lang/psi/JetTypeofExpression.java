package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetTypeofExpression extends JetExpression {
    public JetTypeofExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTypeofExpression(this);
    }

    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }
}
