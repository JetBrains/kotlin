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
public class JetDelegatorToThisCall extends JetDelegationSpecifier implements JetCall {

    public JetDelegatorToThisCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationToThisCall(this);
    }

    @Nullable
    public JetArgumentList getValueArgumentList() {
        return (JetArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetArgument> getValueArguments() {
        JetArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetArgument>emptyList();
    }

    @NotNull
    @Override
    public List<JetExpression> getFunctionLiteralArguments() {
        return Collections.emptyList();
    }

    public JetReferenceExpression getThisReference() {
        return findChildByClass(JetThisReferenceExpression.class);
    }
}
