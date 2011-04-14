package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public interface JetCall {
    @Nullable
    JetArgumentList getValueArgumentList();

    @NotNull
    List<JetArgument> getValueArguments();

    @NotNull
    List<JetExpression> getFunctionLiteralArguments();
}
