package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetDelegatorToThisCall extends JetDelegationSpecifier {
    public JetDelegatorToThisCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationToThisCall(this);
    }

    public JetArgumentList getArgumentList() {
        return (JetArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    public List<JetArgument> getArguments() {
        JetArgumentList list = getArgumentList();
        return list != null ? list.getArguments() : Collections.<JetArgument>emptyList();
    }
}
