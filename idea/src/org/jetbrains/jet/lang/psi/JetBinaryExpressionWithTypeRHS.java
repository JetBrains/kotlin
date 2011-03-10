package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

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

    @Nullable @IfNotParsed
    public JetTypeReference getRight() {
        ASTNode node = getOperationReference().getNode();
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
    public JetReferenceExpression getOperationReference() {
        return (JetReferenceExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }

}
