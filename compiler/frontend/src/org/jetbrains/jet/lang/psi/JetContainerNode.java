package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
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

    @Override // for visibility
    protected PsiElement findChildByType(IElementType type) {
        return super.findChildByType(type);
    }
}
