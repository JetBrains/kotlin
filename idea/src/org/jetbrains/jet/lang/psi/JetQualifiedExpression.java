package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public abstract class JetQualifiedExpression extends JetExpression {
    public JetQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }
}
