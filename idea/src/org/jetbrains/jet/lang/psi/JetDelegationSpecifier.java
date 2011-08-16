package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetDelegationSpecifier extends JetElement{
    public JetDelegationSpecifier(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitDelegationSpecifier(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationSpecifier(this, data);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
