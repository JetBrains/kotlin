package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetExpressionPattern extends JetPattern {
    public JetExpressionPattern(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public JetExpression getExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitExpressionPattern(this);
    }
}
