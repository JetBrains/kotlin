package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitClassBody(this);
    }
}
