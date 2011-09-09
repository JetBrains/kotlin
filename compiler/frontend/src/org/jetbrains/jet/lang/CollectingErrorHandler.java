package org.jetbrains.jet.lang;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

/**
 * @author abreslav
 */
public class CollectingErrorHandler extends ErrorHandler {
    private final List<JetDiagnostic> diagnostics;

    public CollectingErrorHandler() {
        this(Lists.<JetDiagnostic>newArrayList());
    }

    public CollectingErrorHandler(List<JetDiagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public List<JetDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    @Override
    public void unresolvedReference(@NotNull JetReferenceExpression referenceExpression) {
        diagnostics.add(new JetDiagnostic.UnresolvedReferenceError(referenceExpression));
    }

    @Override
    public void typeMismatch(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
        diagnostics.add(new JetDiagnostic.TypeMismatchError(expression, expectedType, actualType));
    }

    @Override
    public void redeclaration(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
        diagnostics.add(new JetDiagnostic.RedeclarationError(existingDescriptor, redeclaredDescriptor));
    }

    @Override
    public void genericError(@NotNull ASTNode node, @NotNull String errorMessage) {
        diagnostics.add(new JetDiagnostic.GenericError(node, errorMessage));
    }

    @Override
    public void genericWarning(@NotNull ASTNode node, @NotNull String message) {
        diagnostics.add(new JetDiagnostic.GenericWarning(node, message));
    }
}
