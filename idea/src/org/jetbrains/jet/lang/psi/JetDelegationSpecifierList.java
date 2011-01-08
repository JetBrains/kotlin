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
    public void accept(JetVisitor visitor) {
        visitor.visitDelegationSpecifierList(this);
    }

    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDelegationSpecifier.class);
    }
}
