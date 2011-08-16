package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetBinaryExpression extends JetExpression {
    public JetBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitBinaryExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitBinaryExpression(this, data);
    }

    @NotNull
    public JetExpression getLeft() {
        JetExpression left = findChildByClass(JetExpression.class);
        assert left != null;
        return left;
    }

    @Nullable @IfNotParsed
    public JetExpression getRight() {
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetExpression) {
                return (JetExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @NotNull
    public JetSimpleNameExpression getOperationReference() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }

    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }

}
