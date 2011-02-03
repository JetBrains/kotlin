package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public abstract class JetLoopExpression extends JetExpression {
    public JetLoopExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public JetExpression getBody() {
        return findExpressionUnder(JetNodeTypes.BODY);
    }
}
