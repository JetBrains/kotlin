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
public class JetTypeParameterListOwner extends JetNamedDeclaration {
    public JetTypeParameterListOwner(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public JetTypeParameterList getTypeParameterList() {
        return (JetTypeParameterList) findChildByType(JetNodeTypes.TYPE_PARAMETER_LIST);
    }

    @Nullable
    public JetTypeConstraintList getTypeConstraintList() {
        return (JetTypeConstraintList) findChildByType(JetNodeTypes.TYPE_CONSTRAINT_LIST);
    }

    @NotNull
    public List<JetTypeConstraint> getTypeConstaints() {
        JetTypeConstraintList typeConstraintList = getTypeConstraintList();
        if (typeConstraintList == null) {
            return Collections.emptyList();
        }
        return typeConstraintList.getConstraints();
    }

    @NotNull
    public List<JetTypeParameter> getTypeParameters() {
        JetTypeParameterList list = getTypeParameterList();
        if (list == null) return Collections.emptyList();

        return list.getParameters();
    }
}
