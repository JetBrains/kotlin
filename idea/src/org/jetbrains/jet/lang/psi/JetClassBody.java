package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetClassBody extends JetElement {
    public JetClassBody(@NotNull ASTNode node) {
        super(node);
    }

    public List<JetDeclaration> getDeclarations() {
        return findChildrenByType(JetNodeTypes.DECLARATIONS);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            ((JetVisitor) visitor).visitClassBody(this);
        }
        else {
            visitor.visitElement(this);
        }
    }
}
