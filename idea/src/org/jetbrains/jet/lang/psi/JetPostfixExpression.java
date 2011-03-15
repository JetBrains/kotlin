package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author max
 */
public class JetPostfixExpression extends JetExpression {
    public JetPostfixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPostfixExpression(this);
    }

    @NotNull
    public JetExpression getBaseExpression() {
        JetExpression answer = findChildByClass(JetExpression.class);
        assert answer != null;
        return answer;
    }

    @NotNull
    public JetSimpleNameExpression getOperationSign() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }
}
