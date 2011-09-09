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
public class JetDelegatorToThisCall extends JetDelegationSpecifier implements JetCallElement {

    public JetDelegatorToThisCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitDelegationToThisCall(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationToThisCall(this, data);
    }

    @Override
    public JetExpression getCalleeExpression() {
        return getThisReference();
    }

    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
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
        return Collections.emptyList();
    }

    @Override
    public JetTypeArgumentList getTypeArgumentList() {
        return null;
    }

    public JetReferenceExpression getThisReference() {
        return findChildByClass(JetThisReferenceExpression.class);
    }
}
