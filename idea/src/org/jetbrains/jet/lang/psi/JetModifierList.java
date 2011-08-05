package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetModifierList extends JetElement {
    public JetModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitModifierList(this);
    }

    @NotNull
    public List<JetAnnotation> getAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    public List<JetAnnotationEntry> getAnnotationEntries() {
        List<JetAnnotationEntry> answer = null;
        for (JetAnnotation annotation : getAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }

    public boolean hasModifier(JetToken token) {
        return getModifierNode(token) != null;
    }

    @Nullable
    public ASTNode getModifierNode(JetToken token) {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            if (node.getElementType() == token) return node;
            node = node.getTreeNext();
        }
        return null;
    }
}
