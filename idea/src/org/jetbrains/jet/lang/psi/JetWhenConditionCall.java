package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetWhenConditionCall extends JetWhenCondition {
    public JetWhenConditionCall(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isSafeCall() {
        return getNode().findChildByType(JetTokens.SAFE_ACCESS) != null;
    }

    @NotNull
    public ASTNode getOperationTokenNode() {
        return getNode().findChildByType(TokenSet.create(JetTokens.SAFE_ACCESS, JetTokens.DOT));
    }

    @Nullable @IfNotParsed
    public JetExpression getCallSuffixExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitWhenConditionCall(this);
    }
}
