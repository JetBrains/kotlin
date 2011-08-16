package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author max
 */
public class JetBlockExpression extends JetExpression {
    public JetBlockExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitBlockExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @NotNull
    public List<JetElement> getStatements() {
        return Arrays.asList(findChildrenByClass(JetElement.class));
    }
}
