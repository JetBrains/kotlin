package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetNamespace extends JetNamedDeclaration {
    public JetNamespace(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitNamespace(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNamespace(this, data);
    }

    public List<JetDeclaration> getDeclarations() {
        PsiElement body = findChildByType(JetNodeTypes.NAMESPACE_BODY);
        return PsiTreeUtil.getChildrenOfTypeAsList(body != null ? body : this, JetDeclaration.class);
    }

    public List<JetImportDirective> getImportDirectives() {
        PsiElement body = findChildByType(JetNodeTypes.NAMESPACE_BODY);
        return PsiTreeUtil.getChildrenOfTypeAsList(body != null ? body : this, JetImportDirective.class);
    }

    public String getName() {
        String name = super.getName();
        return name == null ? "" : name;
    }

    @NotNull
    public JetNamespaceHeader getHeader() {
        return (JetNamespaceHeader) findChildByType(JetNodeTypes.NAMESPACE_HEADER);
    }

    @Override
    public PsiElement getNameIdentifier() {
        return getHeader().getNameIdentifier();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

}
