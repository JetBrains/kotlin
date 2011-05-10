package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author max
 */
public abstract class JetTypeElement extends JetElement {
    public JetTypeElement(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public abstract List<JetTypeReference> getTypeArgumentsAsTypes();

}
