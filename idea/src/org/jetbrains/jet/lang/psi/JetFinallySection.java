package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetFinallySection extends JetElement {
    public JetFinallySection(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitFinallySection(this);
    }

    public JetExpression getFinalExpression() {
        return findChildByClass(JetExpression.class);
    }
}
