package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetSelfType extends JetTypeElement {
    public JetSelfType(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        return Collections.emptyList();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitSelfType(this);
    }
}
