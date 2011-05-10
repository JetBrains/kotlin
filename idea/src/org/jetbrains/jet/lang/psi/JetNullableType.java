package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author max
 */
public class JetNullableType extends JetTypeElement {
    public JetNullableType(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        return getInnerType().getTypeArgumentsAsTypes();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitNullableType(this);
    }

    @NotNull
    public JetTypeElement getInnerType() {
        return findChildByClass(JetTypeElement.class);
    }
}
