package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
 * @author abreslav
 */
public interface BindingTrace {
    BindingTrace DUMMY = new BindingTrace() {
        @Override
        public void recordExpressionType(JetExpression expression, Type type) {
        }

        @Override
        public void recordResolutionResult(JetReferenceExpression expression, DeclarationDescriptor descriptor) {

        }
    };

    void recordExpressionType(JetExpression expression, Type type);

    void recordResolutionResult(JetReferenceExpression expression, DeclarationDescriptor descriptor);
}
