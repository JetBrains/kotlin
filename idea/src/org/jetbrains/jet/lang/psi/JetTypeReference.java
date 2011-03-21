package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetTypeReference extends JetElement {
    public JetTypeReference(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTypeReference(this);
    }

    public List<JetAttributeAnnotation> getAttributeAnnotations() {
        return findChildrenByType(JetNodeTypes.ATTRIBUTE_ANNOTATION);
    }

    @Nullable
    public JetTypeElement getTypeElement() {
        return findChildByClass(JetTypeElement.class);
    }

    public List<JetAttribute> getAttributes() {
        List<JetAttribute> answer = null;
        for (JetAttributeAnnotation annotation : getAttributeAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAttribute>();
            answer.addAll(annotation.getAttributes());
        }
        return answer != null ? answer : Collections.<JetAttribute>emptyList();
    }
}
