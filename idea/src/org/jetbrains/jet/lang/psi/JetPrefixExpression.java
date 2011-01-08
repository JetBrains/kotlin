package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetPrefixExpression extends JetExpression {
    public JetPrefixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPrefixExpression(this);
    }

    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }

    @NotNull
    public ASTNode getOperationTokenNode() {
        ASTNode operationNode = getNode().findChildByType(JetTokens.OPERATIONS);
        assert operationNode != null;
        return operationNode;
    }

    @Nullable
    public JetToken getOperationSign() {
        return (JetToken) getOperationTokenNode().getElementType();
    }
}
