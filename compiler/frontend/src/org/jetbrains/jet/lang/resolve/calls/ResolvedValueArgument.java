package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

/**
* @author abreslav
*/
public interface ResolvedValueArgument {
    @NotNull
    List<JetExpression> getArgumentExpressions();

}
