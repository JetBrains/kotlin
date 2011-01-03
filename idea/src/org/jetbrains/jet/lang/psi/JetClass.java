package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetClass extends JetNamedDeclaration{
    public JetClass(@NotNull ASTNode node) {
        super(node);
    }

    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitClass(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
