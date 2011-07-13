package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetAnnotatedExpression extends JetExpression {
    public JetAnnotatedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitAnnotatedExpression(this);
    }

    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }

    public List<JetAnnotation> getAttributeAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    public List<JetAnnotationEntry> getAttributes() {
        List<JetAnnotationEntry> answer = null;
        for (JetAnnotation annotation : getAttributeAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }
}
