package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetTypedef extends JetTypeParameterListOwner {
    public JetTypedef(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTypedef(this);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
