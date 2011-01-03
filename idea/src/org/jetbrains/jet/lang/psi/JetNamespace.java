package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetNamespace extends JetDeclaration {
    public JetNamespace(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitNamespace(this);
        }
        else {
            visitor.visitElement(this);
        }
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
        PsiElement nameNode = findChildByType(JetNodeTypes.NAMESPACE_NAME);
        return nameNode != null ? nameNode.getText() : "";
    }
}
