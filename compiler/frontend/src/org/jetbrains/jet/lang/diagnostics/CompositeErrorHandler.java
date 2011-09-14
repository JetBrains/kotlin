package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class CompositeErrorHandler extends ErrorHandler {
    private final ErrorHandler[] handlers;

    public CompositeErrorHandler(ErrorHandler... handlers) {
        this.handlers = handlers;
    }

    @Override
    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
        for (ErrorHandler handler : handlers) {
            handler.unresolvedReference(referenceExpression);
        }
    }

    @Override
    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
        for (ErrorHandler handler : handlers) {
            handler.typeMismatch(expression, expectedType, actualType);
        }
    }

    @Override
    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
        for (ErrorHandler handler : handlers) {
            handler.redeclaration(existingDescriptor, redeclaredDescriptor);
        }
    }

    @Override
    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
        for (ErrorHandler handler : handlers) {
            handler.genericError(node, errorMessage);
        }
    }

    @Override
    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
        for (ErrorHandler handler : handlers) {
            handler.genericWarning(node, message);
        }
    }
}
