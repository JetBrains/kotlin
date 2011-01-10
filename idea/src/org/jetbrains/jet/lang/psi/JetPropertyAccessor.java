package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetPropertyAccessor extends JetDeclaration {
    public JetPropertyAccessor(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPropertyAccessor(this);
    }

    public boolean isSetter() {
        return findChildByType(JetTokens.SET_KEYWORD) != null;
    }

    public boolean isGetter() {
        return findChildByType(JetTokens.GET_KEYWORD) != null;
    }

    @Nullable
    public JetParameter getParameter() {
        return (JetParameter) findChildByType(JetNodeTypes.VALUE_PARAMETER);
    }

    @Nullable
    public JetExpression getBody() {
        return findChildByClass(JetExpression.class);
    }
}
