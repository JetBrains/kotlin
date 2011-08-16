package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetFinallySection extends JetElement {
    public JetFinallySection(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitFinallySection(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitFinallySection(this, data);
    }

    public JetBlockExpression getFinalExpression() {
        return (JetBlockExpression) findChildByType(JetNodeTypes.BLOCK);
    }
}
