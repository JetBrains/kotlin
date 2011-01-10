package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetDoWhileExpression extends JetExpression {
    public JetDoWhileExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitDoWhileExpression(this);
    }

    @Nullable(IF_NOT_PARSED)
    public JetExpression getCondition() {
        return findExpressionUnder(JetNodeTypes.CONDITION);
    }

    @Nullable(IF_NOT_PARSED)
    public JetExpression getBody() {
        return findExpressionUnder(JetNodeTypes.BODY);
    }
}
