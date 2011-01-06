package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetPostfixExpression extends JetExpression {
    public JetPostfixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPostfixExpression(this);
    }

    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }
}
