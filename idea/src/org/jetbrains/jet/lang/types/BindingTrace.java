package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetTypeReference;

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

        @Override
        public void recordTypeResoltion(JetTypeReference typeReference, Type type) {

        }
    };

    void recordExpressionType(JetExpression expression, Type type);

    void recordResolutionResult(JetReferenceExpression expression, DeclarationDescriptor descriptor);

    void recordTypeResoltion(JetTypeReference typeReference, Type type);
}
