package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetWhenConditionIsPattern extends JetWhenCondition {
    public JetWhenConditionIsPattern(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isNegated() {
        return getNode().findChildByType(JetTokens.NOT_IS) != null;
    }

    @Nullable @IfNotParsed
    public JetPattern getPattern() {
        return findChildByClass(JetPattern.class);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitWhenConditionIsPattern(this);
    }
}
