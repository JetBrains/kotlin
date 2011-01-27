package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public abstract class JetDeclaration extends JetElement {
    public JetDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public JetModifierList getModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.MODIFIER_LIST);
    }
}
