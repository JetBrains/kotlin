package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetTupleType extends JetTypeElement {
    public JetTupleType(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTupleType(this);
    }

    @NotNull
    public List<JetTypeReference> getComponentTypeRefs() {
        return findChildrenByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
