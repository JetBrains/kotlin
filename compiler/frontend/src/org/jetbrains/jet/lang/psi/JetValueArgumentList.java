package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetValueArgumentList extends JetElement {
    public JetValueArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitValueArgumentList(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitValueArgumentList(this, data);
    }

    public List<JetValueArgument> getArguments() {
        return findChildrenByType(JetNodeTypes.VALUE_ARGUMENT);
    }
}
