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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitNamespace(this);
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
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier.getText() : "";
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(JetNodeTypes.NAMESPACE_NAME);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        throw new UnsupportedOperationException(); // TODO
    }

    public String getFQName() {
        JetNamespace parent = PsiTreeUtil.getParentOfType(this, JetNamespace.class);
        if (parent != null) {
            return parent.getFQName() + "." + getName();
        }
        return getName(); // TODO: Must include module root namespace
    }
}
