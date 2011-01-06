package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetDelegatorByExpressionSpecifier extends JetDelegationSpecifier {
    public JetDelegatorByExpressionSpecifier(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationByExpressionSpecifier(this);
    }
}
