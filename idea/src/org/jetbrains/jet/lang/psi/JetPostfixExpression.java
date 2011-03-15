package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetPostfixExpression extends JetUnaryExpression {
    public JetPostfixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public JetExpression getBaseExpression() {
        JetExpression answer = findChildByClass(JetExpression.class);
        assert answer != null;
        return answer;
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPostfixExpression(this);
    }

}
