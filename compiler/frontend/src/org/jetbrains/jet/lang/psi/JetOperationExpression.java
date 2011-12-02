package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface JetOperationExpression {
    @NotNull
    JetSimpleNameExpression getOperationReference();
}
