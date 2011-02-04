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
    public void accept(JetVisitor visitor) {
        visitor.visitBlockExpression(this);
    }

    @NotNull
    public List<JetElement> getStatements() {
        return Arrays.asList(findChildrenByClass(JetElement.class));
    }
}
