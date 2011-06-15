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
public class JetCallExpression extends JetExpression implements JetCall {
    public JetCallExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitCallExpression(this);
    }

    @Nullable
    public JetExpression getCalleeExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    @Nullable
    public JetArgumentList getValueArgumentList() {
        return (JetArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Nullable
    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<JetExpression> getFunctionLiteralArguments() {
        return findChildrenByType(JetNodeTypes.FUNCTION_LITERAL);
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

    @Override
    @NotNull
    public List<JetArgument> getValueArguments() {
        JetArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetArgument>emptyList();
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList list = getTypeArgumentList();
        return list != null ? list.getArguments() : Collections.<JetTypeProjection>emptyList();
    }
}
