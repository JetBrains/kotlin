package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public class ExpressionValueArgument implements ResolvedValueArgument {
    private final JetExpression expression;

    public ExpressionValueArgument(@Nullable JetExpression expression) {
        this.expression = expression;
    }

    // Nullable when something like f(a, , b) was in the source code
    @Nullable
    public JetExpression getExpression() {
        return expression;
    }

    @NotNull
    @Override
    public List<JetExpression> getArgumentExpressions() {
        if (expression == null) return Collections.emptyList();
        return Collections.singletonList(expression);
    }

    @Override
    public String toString() {
        return expression.getText();
    }
}
