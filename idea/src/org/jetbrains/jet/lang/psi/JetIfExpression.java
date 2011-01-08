package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetIfExpression extends JetExpression {
    public JetIfExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitIfExpression(this);
    }

    @Nullable
    public JetExpression getCondition() {
        return findExpressionUnder(JetNodeTypes.CONDITION);
    }

    public JetExpression getThen() {
        return findExpressionUnder(JetNodeTypes.THEN);
    }

    public JetExpression getElse() {
        return findExpressionUnder(JetNodeTypes.ELSE);
    }
}
