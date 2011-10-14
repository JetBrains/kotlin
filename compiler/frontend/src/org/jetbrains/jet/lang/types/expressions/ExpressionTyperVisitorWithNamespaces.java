package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetRootNamespaceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.JetModuleUtil;
import org.jetbrains.jet.lang.types.JetType;

/**
* @author abreslav
*/
public class ExpressionTyperVisitorWithNamespaces extends ExpressionTyperVisitor {
    public static final ExpressionTyperVisitorWithNamespaces INSTANCE = new ExpressionTyperVisitorWithNamespaces();

    private ExpressionTyperVisitorWithNamespaces() {}

    @Override
    public boolean isNamespacePosition() {
        return true;
    }

    @Override
    public JetType visitRootNamespaceExpression(JetRootNamespaceExpression expression, ExpressionTypingContext context) {
        return context.getServices().checkType(JetModuleUtil.getRootNamespaceType(expression), expression, context);
    }

    @Override
    protected boolean furtherNameLookup(@NotNull JetSimpleNameExpression expression, @NotNull String referencedName, @NotNull JetType[] result, ExpressionTypingContext context) {
        result[0] = lookupNamespaceType(expression, referencedName, context);
        return result[0] != null;
    }

}
