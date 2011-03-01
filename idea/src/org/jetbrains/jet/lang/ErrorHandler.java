package org.jetbrains.jet.lang;

import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
 * @author abreslav
 */
public class ErrorHandler {
    public static final ErrorHandler DO_NOTHING = new ErrorHandler();
    public static final ErrorHandler THROW_EXCEPTION = new ErrorHandler() {
        @Override
        public void unresolvedReference(JetReferenceExpression referenceExpression) {
            throw new IllegalStateException("Unresolved reference: " + referenceExpression.getReferencedName());
        }
    };

    public void unresolvedReference(JetReferenceExpression referenceExpression) {
    }
}
