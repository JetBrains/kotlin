package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class JetReturnExpression extends JetLabelQualifiedExpression {
    public JetReturnExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitReturnExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitReturnExpression(this, data);
    }

    @Nullable
    public JetExpression getReturnedExpression() {
        return findChildByClass(JetExpression.class);
    }
}
