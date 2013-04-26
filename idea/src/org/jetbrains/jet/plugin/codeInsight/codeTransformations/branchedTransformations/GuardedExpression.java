package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

public class GuardedExpression {
    @NotNull
    private final JetExpression condition;

    @NotNull
    private final JetExpression baseExpression;

    public GuardedExpression(@NotNull JetExpression condition, @NotNull JetExpression baseExpression) {
        this.condition = condition;
        this.baseExpression = baseExpression;
    }

    @NotNull
    public JetExpression getCondition() {
        return condition;
    }

    @NotNull
    public JetExpression getBaseExpression() {
        return baseExpression;
    }
}
