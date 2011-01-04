package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetTypeParameterList extends JetElement {
    public JetTypeParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public List<JetTypeParameter> getParameters() {
        return findChildrenByType(JetNodeTypes.TYPE_PARAMETER);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTypeParameterList(this);
    }
}
