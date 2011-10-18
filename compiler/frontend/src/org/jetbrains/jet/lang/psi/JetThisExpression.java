package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetThisExpression extends JetLabelQualifiedExpression {

    public JetThisExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitThisExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitThisExpression(this, data);
    }

    /**
     * class A : B, C {
     *     override fun foo() {
     *         this<B>.foo()
     *         this<C>.foo()
     *     }
     * }
     */
    @Nullable
    public JetTypeReference getSuperTypeQualifier() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @NotNull
    public JetReferenceExpression getThisReference() {
        return (JetReferenceExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

}
