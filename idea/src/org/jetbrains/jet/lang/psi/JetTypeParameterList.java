package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
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

    @Nullable
    public JetTypeConstraintList getTypeConstraintList() {
        return (JetTypeConstraintList) findChildByType(JetNodeTypes.TYPE_CONSTRAINT_LIST);
    }

    @NotNull
    public List<JetTypeConstraint> getAdditionalConstraints() {
        JetTypeConstraintList list = getTypeConstraintList();
        return list != null ? list.getConstraints() : Collections.<JetTypeConstraint>emptyList();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTypeParameterList(this);
    }
}
