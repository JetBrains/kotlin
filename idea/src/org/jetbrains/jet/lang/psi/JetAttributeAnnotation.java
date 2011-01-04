package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetAttributeAnnotation extends JetElement {
    public JetAttributeAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitAttributeAnnotation(this);
    }

    public List<JetAttribute> getAttributes() {
        return findChildrenByType(JetNodeTypes.ATTRIBUTE);
    }
}
