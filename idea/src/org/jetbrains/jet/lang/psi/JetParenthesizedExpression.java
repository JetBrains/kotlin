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
    public void accept(JetVisitor visitor) {
        visitor.visitParenthesizedExpression(this);
    }

    @Nullable @IfNotParsed
    public JetExpression getExpression() {
        return findChildByClass(JetExpression.class);
    }
}
