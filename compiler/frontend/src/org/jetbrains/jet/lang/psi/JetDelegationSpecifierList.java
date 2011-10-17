package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author max
 */
public class JetDelegationSpecifierList extends JetElement {
    public JetDelegationSpecifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitDelegationSpecifierList(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationSpecifierList(this, data);
    }

    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDelegationSpecifier.class);
    }
}
