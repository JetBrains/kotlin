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
public class JetNewExpression extends JetExpression implements JetCall {
    public JetNewExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitNewExpression(this);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @Override
    @Nullable
    public JetArgumentList getValueArgumentList() {
        return (JetArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<JetArgument> getValueArguments() {
        JetArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetArgument>emptyList();
    }

    @Override
    @NotNull
    public List<JetExpression> getFunctionLiteralArguments() {
        return findChildrenByType(JetNodeTypes.FUNCTION_LITERAL);
    }

    @NotNull
    @Override
    public List<JetTypeProjection> getTypeArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

}
