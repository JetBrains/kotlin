package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetVisitor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
/*package*/ abstract class ExpressionTypingVisitor extends JetVisitor<JetType, ExpressionTypingContext> {

    protected final ExpressionTypingInternals facade;

    protected ExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        this.facade = facade;
    }
}
