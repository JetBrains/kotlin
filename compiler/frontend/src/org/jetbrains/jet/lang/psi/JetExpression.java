package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeType;

/**
 * @author max
 */
public abstract class JetExpression extends JetElement {
    public JetExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitExpression(this, data);
    }

    protected JetExpression findExpressionUnder(JetNodeType type) {
        JetContainerNode containerNode = (JetContainerNode) findChildByType(type);
        if (containerNode == null) return null;

        return containerNode.findChildByClass(JetExpression.class);
    }
}
