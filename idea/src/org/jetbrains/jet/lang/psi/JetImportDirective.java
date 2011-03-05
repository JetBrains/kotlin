package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetImportDirective extends JetElement {
    public JetImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitImportDirective(this);
    }

    public boolean isAbsoluteInRootNamespace() {
        return findChildByType(JetTokens.NAMESPACE_KEYWORD) != null;
    }

    @Nullable @IfNotParsed
    public JetExpression getImportedReference() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable
    public ASTNode getAliasNameNode() {
        boolean asPassed = false;
        ASTNode childNode = getNode().getFirstChildNode();
        while (childNode != null) {
            IElementType tt = childNode.getElementType();
            if (tt == JetTokens.AS_KEYWORD) asPassed = true;
            if (asPassed && tt == JetTokens.IDENTIFIER) {
                return childNode;
            }

            childNode = childNode.getTreeNext();
        }
        return null;
    }

    @Nullable
    public String getAliasName() {
        ASTNode aliasNameNode = getAliasNameNode();
        if (aliasNameNode == null) {
            return null;
        }
        return aliasNameNode.getText();
    }

    public boolean isAllUnder() {
        return getNode().findChildByType(JetTokens.MUL) != null;
    }
}
