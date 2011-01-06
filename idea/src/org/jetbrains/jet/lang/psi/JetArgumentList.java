package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetArgumentList extends JetElement {
    public JetArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitArgumentList(this);
    }

    public List<JetArgument> getArguments() {
        return findChildrenByType(JetNodeTypes.VALUE_ARGUMENT);
    }
}
