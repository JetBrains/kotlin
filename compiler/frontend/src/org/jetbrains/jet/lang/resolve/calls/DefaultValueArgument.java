package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public class DefaultValueArgument implements ResolvedValueArgument {
    public static final DefaultValueArgument DEFAULT = new DefaultValueArgument();

    private DefaultValueArgument() {}

    @NotNull
    @Override
    public List<JetExpression> getArgumentExpressions() {
        return Collections.emptyList(); //throw new UnsupportedOperationException("Look into the default value of the parameter");
    }
}
