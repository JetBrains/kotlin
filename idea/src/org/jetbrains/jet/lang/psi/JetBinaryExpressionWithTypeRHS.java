package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetBinaryExpressionWithTypeRHS extends JetExpression {
    public JetBinaryExpressionWithTypeRHS(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitBinaryWithTypeRHSExpression(this);
    }

    @NotNull
    public JetExpression getLeft() {
        JetExpression left = findChildByClass(JetExpression.class);
        assert left != null;
        return left;
    }

    @Nullable(IF_NOT_PARSED)
    public JetTypeReference getRight() {
        ASTNode node = getOperationTokenNode();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetTypeReference) {
                return (JetTypeReference) psi;
            }
            node = node.getTreeNext();
        }

        return null;
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
