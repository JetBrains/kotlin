package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetUserType extends JetTypeElement {
    public JetUserType(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitUserType(this);
    }

    public JetReferenceExpression getReferenceExpression() {
        return (JetReferenceExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }
}
