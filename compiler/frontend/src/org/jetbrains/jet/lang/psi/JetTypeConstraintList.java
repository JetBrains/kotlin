package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetTypeConstraintList extends JetElement {
    public JetTypeConstraintList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeConstraintList(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraintList(this, data);
    }

    @NotNull
    public List<JetTypeConstraint> getConstraints() {
        return findChildrenByType(JetNodeTypes.TYPE_CONSTRAINT);
    }
}
