package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
* @author abreslav
*/
public class ExpressionValueArgument implements ResolvedValueArgument {
    private final JetExpression expression;

    public ExpressionValueArgument(@NotNull JetExpression expression) {
        this.expression = expression;
    }

    @NotNull
    public JetExpression getExpression() {
        return expression;
    }
}
