package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetObjectDeclarationName extends JetNamedDeclaration {

    public JetObjectDeclarationName(@NotNull ASTNode node) {
        super(node);
    }
}
