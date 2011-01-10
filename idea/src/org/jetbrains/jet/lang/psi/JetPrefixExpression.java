package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    public JetExpression getBaseExpression() {
        JetExpression answer = findChildByClass(JetExpression.class);
        assert answer != null;
        return answer;
    }

    @NotNull
    public ASTNode getOperationTokenNode() {
        ASTNode operationNode = getNode().findChildByType(JetTokens.OPERATIONS);
        assert operationNode != null;
        return operationNode;
    }

    @NotNull
    public JetToken getOperationSign() {
        return (JetToken) getOperationTokenNode().getElementType();
    }
}
