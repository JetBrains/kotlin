package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetUserType extends JetTypeElement {
    public JetUserType(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitUserType(this);
    }

    @NotNull
    public String getReferencedName() {
        StringBuilder answer = new StringBuilder();
        ASTNode childNode = getNode().getFirstChildNode();
        while (childNode != null) {
            IElementType tt = childNode.getElementType();
            if (tt == JetTokens.IDENTIFIER || tt == JetTokens.DOT || tt == JetTokens.NAMESPACE_KEYWORD) {
                answer.append(childNode.getText());
            }
            childNode = childNode.getTreeNext();
        }
        return answer.toString();
    }

    @Nullable
    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetTypeReference> getTypeArguments() {
        JetTypeArgumentList list = getTypeArgumentList();
        return list != null ? list.getArguments() : Collections.<JetTypeReference>emptyList();
    }
}
