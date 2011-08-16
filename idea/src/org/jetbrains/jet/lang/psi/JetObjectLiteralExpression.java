package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetObjectLiteralExpression extends JetExpression {
    public JetObjectLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitObjectLiteralExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitObjectLiteralExpression(this, data);
    }

    @NotNull
    public JetObjectDeclaration getObjectDeclaration() {
        return (JetObjectDeclaration) findChildByType(JetNodeTypes.OBJECT_DECLARATION);
    }
}
