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
    public void accept(JetVisitor visitor) {
        visitor.visitInitializerList(this);
    }

    @NotNull
    public List<JetDelegationSpecifier> getInitializers() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDelegationSpecifier.class);
    }
}
