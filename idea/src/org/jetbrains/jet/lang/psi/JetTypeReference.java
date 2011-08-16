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
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeReference(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitTypeReference(this, data);
    }

    public List<JetAnnotation> getAttributeAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    @Nullable
    public JetTypeElement getTypeElement() {
        return findChildByClass(JetTypeElement.class);
    }

    public List<JetAnnotationEntry> getAnnotations() {
        List<JetAnnotationEntry> answer = null;
        for (JetAnnotation annotation : getAttributeAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }
}
