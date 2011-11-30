package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

/**
 * @author abreslav
 */
public class JetNamespaceHeader extends JetElement {
    public JetNamespaceHeader(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public List<JetSimpleNameExpression> getParentNamespaceNames() {
        return findChildrenByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable
    public PsiElement getNameIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @Override
    public String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? "" : nameIdentifier.getText();
    }
}
