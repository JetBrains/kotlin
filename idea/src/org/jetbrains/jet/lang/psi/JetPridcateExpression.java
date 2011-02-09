package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetPridcateExpression extends JetQualifiedExpression {
    public JetPridcateExpression(@NotNull ASTNode node) {
        super(node);
    }
}
