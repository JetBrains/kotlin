package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetWhileExpression extends JetLoopExpression {
    public JetWhileExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitWhileExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitWhileExpression(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getCondition() {
        return findExpressionUnder(JetNodeTypes.CONDITION);
    }
}
