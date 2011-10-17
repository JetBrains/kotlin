package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class JetParenthesizedExpression extends JetExpression {
    public JetParenthesizedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitParenthesizedExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitParenthesizedExpression(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getExpression() {
        return findChildByClass(JetExpression.class);
    }
}
