package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetAnnotatedExpression extends JetExpression {
    public JetAnnotatedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitAnnotatedExpression(this);
    }

    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }
}
