package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
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
        public void genericError(@NotNull ASTNode node, String errorMessage) {
            throw new IllegalStateException(errorMessage);
        }

        @Override
        public void typeMismatch(JetExpression expression, JetType expectedType, JetType actualType) {
            throw new IllegalStateException("Type mismatch: inferred type is " + actualType + " but " + expectedType + " was expected");
        }

        @Override
        public void redeclaration(DeclarationDescriptor existingDescriptor, DeclarationDescriptor redeclaredDescriptor) {
            throw new IllegalStateException("Redeclaration: " + existingDescriptor.getName());
        }
    };

    public void unresolvedReference(JetReferenceExpression referenceExpression) {
    }

    public void typeMismatch(JetExpression expression, JetType expectedType, JetType actualType) {
    }

    public void redeclaration(DeclarationDescriptor existingDescriptor, DeclarationDescriptor redeclaredDescriptor) {
    }

    public void genericError(@NotNull ASTNode node, String errorMessage) {
    }

    public void genericWarning(ASTNode node, String message) {
    }
}
