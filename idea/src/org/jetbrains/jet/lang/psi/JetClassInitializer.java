package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public JetExpression getBody() {
        return findChildByClass(JetExpression.class);
    }
}
