package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetMatchExpression extends JetExpression {
    public JetMatchExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitMatchExpression(this);
    }

    @NotNull
    public JetExpression getTestedExpression() {
        JetExpression left = findChildByClass(JetExpression.class);
        assert left != null;
        return left;
    }

    @Nullable(IF_NOT_PARSED)
    public JetMatchBlock getMatchBlock() {
        return (JetMatchBlock) findChildByType(JetNodeTypes.MATCH_BLOCK);
    }
}
