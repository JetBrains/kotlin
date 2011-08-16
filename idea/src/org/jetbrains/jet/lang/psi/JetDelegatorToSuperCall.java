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
public class JetDelegatorToSuperCall extends JetDelegationSpecifier implements JetCall {
    public JetDelegatorToSuperCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitDelegationToSuperCallSpecifier(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationToSuperCallSpecifier(this, data);
    }

    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<JetExpression> getFunctionLiteralArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeReference typeReference = getTypeReference();
        if (typeReference != null) {
            JetTypeElement typeElement = typeReference.getTypeElement();
            if (typeElement instanceof JetUserType) {
                JetUserType userType = (JetUserType) typeElement;
                return userType.getTypeArguments();
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }
}
