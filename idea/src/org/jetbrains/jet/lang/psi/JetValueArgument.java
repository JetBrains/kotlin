package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetValueArgument extends JetElement {
    public JetValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitArgument(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getArgumentExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable
    public JetValueArgumentName getArgumentName() {
        return (JetValueArgumentName) findChildByType(JetNodeTypes.VALUE_ARGUMENT_NAME);
    }

    public boolean isNamed() {
        return getArgumentName() != null;
    }

    public boolean isOut() {
        return findChildByType(JetTokens.OUT_KEYWORD) != null;
    }

    public boolean isRef() {
        return findChildByType(JetTokens.REF_KEYWORD) != null;
    }
}
