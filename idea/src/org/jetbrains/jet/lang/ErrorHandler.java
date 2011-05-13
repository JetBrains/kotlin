package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
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
        public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
            throw new IllegalStateException("Unresolved reference: " + referenceExpression.getText() +
                                            " at offset "  + referenceExpression.getTextRange().getStartOffset());
        }

        @Override
        public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
            throw new IllegalStateException(errorMessage);
        }

        @Override
        public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
            throw new IllegalStateException("Type mismatch: inferred type is " + actualType + " but " + expectedType + " was expected");
        }

        @Override
        public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
            throw new IllegalStateException("Redeclaration: " + existingDescriptor.getName());
        }
    };

    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
    }

    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
    }

    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
    }

    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
    }

    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
    }
}
