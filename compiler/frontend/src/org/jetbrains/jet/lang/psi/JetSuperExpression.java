package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetSuperExpression extends JetLabelQualifiedInstanceExpression {

    public JetSuperExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitSuperExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSuperExpression(this, data);
    }

    /**
     * class A : B, C {
     *     override fun foo() {
     *         super<B>.foo()
     *         super<C>.foo()
     *     }
     * }
     */
    @Nullable
    public JetTypeReference getSuperTypeQualifier() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
