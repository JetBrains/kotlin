package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetReferenceExpression extends JetExpression {
    private static final TokenSet REFERENCE_TOKENS = TokenSet.create(JetTokens.IDENTIFIER, JetTokens.FIELD_IDENTIFIER);

    public JetReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isAbsoluteInRootNamespace() {
        return findChildByType(JetTokens.NAMESPACE_KEYWORD) != null;
    }

    @Nullable
    public JetReferenceExpression getQualifier() {
        return findChildByClass(JetReferenceExpression.class);
    }

    public String getReferencedName() {
        return getNode().findChildByType(REFERENCE_TOKENS).getText();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }
}
