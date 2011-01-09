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
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationSpecifier(this);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
