package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author Evgeny Gerashchenko
 * @since 2/8/12
 */
public class JetIdeTemplateExpression extends JetExpression {
    public JetIdeTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitIdeTemplateExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitIdeTemplateExpression(this, data);
    }
}
