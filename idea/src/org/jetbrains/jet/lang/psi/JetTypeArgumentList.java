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
    public void accept(JetVisitor visitor) {
        visitor.visitTypeArgumentList(this);
    }

    public List<JetTypeProjection> getArguments() {
        return findChildrenByType(JetNodeTypes.TYPE_PROJECTION);
    }
}
