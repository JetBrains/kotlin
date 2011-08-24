package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public interface Call {
    @Nullable
    JetExpression getCalleeExpression();

    @Nullable
    JetValueArgumentList getValueArgumentList();

    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @NotNull
    List<JetExpression> getFunctionLiteralArguments();

    @NotNull
    List<JetTypeProjection> getTypeArguments();

    @Nullable
    JetTypeArgumentList getTypeArgumentList();
}
