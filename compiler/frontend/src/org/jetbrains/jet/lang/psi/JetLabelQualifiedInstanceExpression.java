package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public abstract class JetLabelQualifiedInstanceExpression extends JetLabelQualifiedExpression {

    public JetLabelQualifiedInstanceExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public JetReferenceExpression getInstanceReference() {
        return (JetReferenceExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }
}
