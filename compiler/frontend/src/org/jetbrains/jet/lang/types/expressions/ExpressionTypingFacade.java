package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface ExpressionTypingFacade {
    @NotNull
    JetType safeGetType(@NotNull JetExpression expression, ExpressionTypingContext context);

    @Nullable
    JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context);
    
    @Nullable
    JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement);
}
