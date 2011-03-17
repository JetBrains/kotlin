package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class JetContainerNode extends JetElement {
    public JetContainerNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override // for visibility
    protected <T> T findChildByClass(Class<T> aClass) {
        return super.findChildByClass(aClass);
    }
}
