package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetDecomposerPropertyList extends JetNamedDeclaration {
    public JetDecomposerPropertyList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitDecomposerPropertyList(this);
    }

    @NotNull
    public List<JetReferenceExpression> getPropertyReferences() {
        return findChildrenByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }
}
