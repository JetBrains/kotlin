package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class JetDelegatorByExpressionSpecifier extends JetDelegationSpecifier {
    public JetDelegatorByExpressionSpecifier(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitDelegationByExpressionSpecifier(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationByExpressionSpecifier(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getDelegateExpression() {
        return findChildByClass(JetExpression.class);
    }
}
