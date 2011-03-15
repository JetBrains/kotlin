package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ErrorHandler {
    public static final ErrorHandler DO_NOTHING = new ErrorHandler();
    public static final ErrorHandler THROW_EXCEPTION = new ErrorHandler() {
        @Override
        public void unresolvedReference(JetReferenceExpression referenceExpression) {
            throw new IllegalStateException("Unresolved reference: " + referenceExpression.getText());
        }

        @Override
        public void structuralError(ASTNode node, String errorMessage) {
            throw new IllegalStateException(errorMessage);
        }

        @Override
        public void typeMismatch(JetExpression expression, JetType expectedType, JetType actualType) {
            throw new IllegalStateException("Type mismatch: inferred type is " + actualType + " but " + expectedType + " was expected");
        }
    };

    public void unresolvedReference(JetReferenceExpression referenceExpression) {
    }

    public void structuralError(ASTNode node, String errorMessage) {
    }

    public void typeMismatch(JetExpression expression, JetType expectedType, JetType actualType) {
    }
}
