package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeType;

/**
 * @author max
 */
public class JetExpression extends JetElement {
    public JetExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitExpression(this);
    }

    protected JetExpression findExpressionUnder(JetNodeType type) {
        JetContainerNode containerNode = (JetContainerNode) findChildByType(type);
        if (containerNode == null) return null;

        return containerNode.findChildByClass(JetExpression.class);
    }
}
