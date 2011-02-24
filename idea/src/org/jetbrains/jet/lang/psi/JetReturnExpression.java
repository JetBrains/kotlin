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
    public void accept(JetVisitor visitor) {
        visitor.visitReturnExpression(this);
    }

    @Nullable
    public JetExpression getReturnedExpression() {
        return findChildByClass(JetExpression.class);
    }
}
