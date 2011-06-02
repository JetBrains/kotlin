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
    public void accept(JetVisitor visitor) {
        visitor.visitObjectLiteralExpression(this);
    }

    @NotNull
    public JetObjectDeclaration getObjectDeclaration() {
        return (JetObjectDeclaration) findChildByType(JetNodeTypes.OBJECT_DECLARATION);
    }
}
