package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type reference element.
 * Underlying token is {@link org.jetbrains.jet.JetNodeTypes#TYPE_REFERENCE}
 *
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
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeReference(this, data);
    }

    public List<JetAnnotation> getAttributeAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    @Nullable
    public JetTypeElement getTypeElement() {
        return findChildByClass(JetTypeElement.class);
    }

    /**
     * Will return not null for type references with internal user type.
     * There could be other JetTypeReferences, see parsing of in {@link org.jetbrains.jet.lang.parsing.JetParsing}
     */
    @Nullable
    public JetUserType getUserType() {
        return findChildByClass(JetUserType.class);
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
