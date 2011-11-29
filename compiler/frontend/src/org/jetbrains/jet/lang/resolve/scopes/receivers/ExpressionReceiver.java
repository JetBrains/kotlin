package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ExpressionReceiver extends AbstractReceiverDescriptor implements ReceiverDescriptor {

    private final JetExpression expression;

    public ExpressionReceiver(@NotNull JetExpression expression, @NotNull JetType type) {
        super(type);
        this.expression = expression;
    }

    @NotNull
    public JetExpression getExpression() {
        return expression;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitExpressionReceiver(this, data);
    }

    @Override
    public String toString() {
        return getType() + " {" + expression + ": " + expression.getText() + "}";
    }
}
