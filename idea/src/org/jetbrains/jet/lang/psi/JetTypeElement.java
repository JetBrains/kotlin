package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public abstract class JetTypeElement extends JetElement {
    public JetTypeElement(@NotNull ASTNode node) {
        super(node);
    }
}
