package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author max
 */
public class JetBinaryExpression extends JetExpression {
    public JetBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitBinaryExpression(this);
    }

    @NotNull
    public JetExpression getLeft() {
        JetExpression left = findChildByClass(JetExpression.class);
        assert left != null;
        return left;
    }

    @Nullable
    public JetExpression getRight() {
        ASTNode node = getOperationTokenNode();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetExpression) {
                return (JetExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    public ASTNode getOperationTokenNode() {
        PsiElement child = getLeft().getNextSibling();

        while (child != null) {
            IElementType tt = child.getNode().getElementType();

            if (JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(tt) || child instanceof PsiErrorElement) {
                child = child.getNextSibling();
            }
            else {
                return child.getNode();
            }
        }

        return null;
    }

    @Nullable
    public JetToken getOperationSign() {
        ASTNode node = getOperationTokenNode();
        return node != null ? (JetToken) node.getElementType() : null;
    }
}
