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
    public List<JetAttributeAnnotation> getAttributeAnnotations() {
        return findChildrenByType(JetNodeTypes.ATTRIBUTE_ANNOTATION);
    }

    public List<JetAttribute> getAttributes() {
        List<JetAttribute> answer = null;
        for (JetAttributeAnnotation annotation : getAttributeAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAttribute>();
            answer.addAll(annotation.getAttributes());
        }
        return answer != null ? answer : Collections.<JetAttribute>emptyList();
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
