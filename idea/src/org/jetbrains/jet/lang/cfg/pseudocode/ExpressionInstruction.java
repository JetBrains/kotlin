package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author abreslav
 */
public abstract class ExpressionInstruction extends Instruction {
    protected final JetExpression expression;

    public ExpressionInstruction(@NotNull JetExpression expression) {
        this.expression = expression;
    }

    @NotNull
    public JetExpression getExpression() {
        return expression;
    }
}
