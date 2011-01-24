package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetReferenceExpression extends JetExpression {
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
        return getNode().findChildByType(JetTokens.IDENTIFIER).getText();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }
}
