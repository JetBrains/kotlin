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
public class ErrorHandlerAdapter extends ErrorHandler {
    protected ErrorHandler worker;

    public ErrorHandlerAdapter(ErrorHandler worker) {
        this.worker = worker;
    }

    @Override
    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
        worker.unresolvedReference(referenceExpression);
    }

    @Override
    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
        worker.typeMismatch(expression, expectedType, actualType);
    }

    @Override
    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
        worker.redeclaration(existingDescriptor, redeclaredDescriptor);
    }

    @Override
    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
        worker.genericError(node, errorMessage);
    }

    @Override
    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
        worker.genericWarning(node, message);
    }
}
