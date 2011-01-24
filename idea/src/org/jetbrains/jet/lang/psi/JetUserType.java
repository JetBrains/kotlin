package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
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

    public boolean isAbsoluteInRootNamespace() {
        return findChildByType(JetTokens.NAMESPACE_KEYWORD) != null;
    }

    @Nullable
    public JetUserType getQualifierType() {
        return (JetUserType) findChildByType(JetNodeTypes.USER_TYPE);
    }

    @Nullable @IfNotParsed
    public ASTNode getTypeNameNode() {
        return getNode().findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable @IfNotParsed
    public String getReferencedName() {
        ASTNode nameNode = getTypeNameNode();
        return nameNode != null ? nameNode.getText() : null;
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
