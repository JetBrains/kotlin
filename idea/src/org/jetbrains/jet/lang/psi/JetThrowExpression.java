package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class JetThrowExpression extends JetExpression {
    public JetThrowExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitThrowExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitThrowExpression(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getThrownExpression() {
        return findChildByClass(JetExpression.class);
    }
}
