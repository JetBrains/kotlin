package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetTypeArgumentList extends JetElement {
    public JetTypeArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTypeArgumentList(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitTypeArgumentList(this, data);
    }

    public List<JetTypeProjection> getArguments() {
        return findChildrenByType(JetNodeTypes.TYPE_PROJECTION);
    }
}
