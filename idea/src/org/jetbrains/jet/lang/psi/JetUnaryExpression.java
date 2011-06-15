package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public abstract class JetUnaryExpression extends JetExpression {
    public JetUnaryExpression(ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public abstract JetExpression getBaseExpression();

    @NotNull
    public JetSimpleNameExpression getOperationSign() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }
}
