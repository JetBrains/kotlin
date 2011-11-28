package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Iterator;
import java.util.List;

/**
* @author abreslav
*/
public class VarargValueArgument implements ResolvedValueArgument {
    private final List<JetExpression> values = Lists.newArrayList();

    @NotNull
    @Override
    public List<JetExpression> getArgumentExpressions() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("vararg:{");
        for (Iterator<JetExpression> iterator = values.iterator(); iterator.hasNext(); ) {
            JetExpression expression = iterator.next();
            builder.append(expression.getText());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.append("}").toString();
    }
}
