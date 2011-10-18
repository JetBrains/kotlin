package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetDoWhileExpression extends JetLoopExpression {
    public JetDoWhileExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitDoWhileExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDoWhileExpression(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getCondition() {
        return findExpressionUnder(JetNodeTypes.CONDITION);
    }

}
