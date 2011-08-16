package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author max
 */
public class JetInitializerList extends JetElement {
    public JetInitializerList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitInitializerList(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitInitializerList(this, data);
    }

    @NotNull
    public List<JetDelegationSpecifier> getInitializers() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDelegationSpecifier.class);
    }
}
