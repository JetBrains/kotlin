package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetParameterList extends JetElement {
    public JetParameterList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitParameterList(this);
    }

    public List<JetParameter> getParameters() {
        return findChildrenByType(JetNodeTypes.VALUE_PARAMETER);
    }
}
