package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetClassInitializer extends JetDeclaration {
    public JetClassInitializer(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitAnonymousInitializer(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitAnonymousInitializer(this, data);
    }

    @NotNull
    public JetExpression getBody() {
        JetExpression body = findChildByClass(JetExpression.class);
        assert body != null;
        return body;
    }
}
