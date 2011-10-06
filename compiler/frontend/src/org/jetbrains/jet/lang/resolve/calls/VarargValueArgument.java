package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

/**
* @author abreslav
*/
public class VarargValueArgument implements ResolvedValueArgument {
    private final List<JetExpression> values = Lists.newArrayList();

    @NotNull
    public List<JetExpression> getValues() {
        return values;
    }
}
