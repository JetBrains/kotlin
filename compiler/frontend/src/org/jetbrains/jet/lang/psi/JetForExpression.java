package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetForExpression extends JetLoopExpression {
    public JetForExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitForExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitForExpression(this, data);
    }

    @Nullable @IfNotParsed
    public JetParameter getLoopParameter() {
        return (JetParameter) findChildByType(JetNodeTypes.LOOP_PARAMETER);
    }

    @Nullable @IfNotParsed
    public JetExpression getLoopRange() {
        return findExpressionUnder(JetNodeTypes.LOOP_RANGE);
    }
}
