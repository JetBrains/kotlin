package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface JetVariableDeclaration extends JetDeclaration, JetWithExpressionInitializer {
    boolean isVar();

    @NotNull
    ASTNode getValOrVarNode();
}
