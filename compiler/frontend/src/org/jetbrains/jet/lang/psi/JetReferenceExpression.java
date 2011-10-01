package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class JetReferenceExpression extends JetExpression {
    public JetReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }
}
