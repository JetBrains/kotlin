package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetWhenEntry extends JetElement {
    public JetWhenEntry(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isElse() {
        return findChildByType(JetTokens.ELSE_KEYWORD) != null;
    }

    public boolean isElseContinue() {
        return isElse() && (findChildByType(JetTokens.DOUBLE_ARROW) == null) && (findChildByType(JetTokens.CONTINUE_KEYWORD) != null);
    }

    @Nullable
    public JetExpression getExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable
    public JetWhenExpression getSubWhen() {
        // TODO: this may be a WHEN that goes after "=>"
        return (JetWhenExpression) findChildByType(JetNodeTypes.WHEN);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitWhenEntry(this);
    }

    @NotNull
    public JetWhenCondition[] getConditions() {
        return findChildrenByClass(JetWhenCondition.class);
    }
}
