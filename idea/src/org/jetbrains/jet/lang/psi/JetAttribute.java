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
public class JetAttribute extends JetElement {
    public JetAttribute(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitAttribute(this);
    }

    @Nullable @IfNotParsed
    public JetUserType getTypeReference() {
        return (JetUserType) findChildByType(JetNodeTypes.USER_TYPE);
    }

    @Nullable
    public JetArgumentList getArgumentList() {
        return (JetArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetArgument> getArguments() {
        JetArgumentList list = getArgumentList();
        return list != null ? list.getArguments() : Collections.<JetArgument>emptyList();
    }

}
