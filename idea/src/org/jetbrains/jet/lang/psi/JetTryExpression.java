package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetTryExpression extends JetExpression {
    public JetTryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTryExpression(this);
    }

    @NotNull
    public JetBlockExpression getTryBlock() {
        return (JetBlockExpression) findChildByType(JetNodeTypes.BLOCK);
    }

    @NotNull
    public List<JetCatchClause> getCatchClauses() {
        return findChildrenByType(JetNodeTypes.CATCH);
    }

    @Nullable
    public JetFinallySection getFinallyBlock() {
        return (JetFinallySection) findChildByType(JetNodeTypes.FINALLY);
    }

}
