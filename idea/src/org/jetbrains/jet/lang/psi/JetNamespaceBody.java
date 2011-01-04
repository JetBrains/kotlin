package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetNamespaceBody extends JetElement {
    public JetNamespaceBody(@NotNull ASTNode node) {
        super(node);
    }

    public List<JetDeclaration> getDeclarations() {
        return findChildrenByType(JetNodeTypes.DECLARATIONS);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitNamespaceBody(this);
    }
}
