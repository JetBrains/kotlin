package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetForExpression extends JetExpression {
    public JetForExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitForExpression(this);
    }

    public JetParameter getLoopParameter() {
        return (JetParameter) findChildByType(JetNodeTypes.LOOP_PARAMETER);
    }

    public JetExpression getLoopRange() {
        return findExpressionUnder(JetNodeTypes.LOOP_RANGE);
    }

    public JetExpression getBody() {
        return findExpressionUnder(JetNodeTypes.THEN);
    }
}
