package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

public class MultiGuardedExpression {
    @NotNull
    private final List<JetExpression> conditions;

    @NotNull
    private final JetExpression baseExpression;

    public MultiGuardedExpression(@NotNull List<JetExpression> conditions, @NotNull JetExpression baseExpression) {
        this.conditions = conditions;
        this.baseExpression = baseExpression;
    }

    @NotNull
    public List<JetExpression> getConditions() {
        return conditions;
    }

    @NotNull
    public JetExpression getBaseExpression() {
        return baseExpression;
    }
}
