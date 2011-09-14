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
public abstract class JetDiagnostic {

    public static class UnresolvedReferenceError extends JetDiagnostic {

        private final JetReferenceExpression referenceExpression;

        public UnresolvedReferenceError(@NotNull JetReferenceExpression referenceExpression) {
            this.referenceExpression = referenceExpression;
        }

        @Override
        public void acceptHandler(@NotNull ErrorHandler handler) {
            handler.unresolvedReference(referenceExpression);
        }

        @NotNull
        public JetReferenceExpression getReferenceExpression() {
            return referenceExpression;
        }
    }

    public static class GenericError extends JetDiagnostic {

        private final ASTNode node;
        private final String message;

        public GenericError(@NotNull ASTNode node, @NotNull String message) {
            this.node = node;
            this.message = message;
        }

        @Override
        public void acceptHandler(@NotNull ErrorHandler handler) {
            handler.genericError(node, message);
        }
    }

    public static class TypeMismatchError extends JetDiagnostic {

        private final JetExpression expression;
        private final JetType expectedType;
        private final JetType actualType;

        public TypeMismatchError(@NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull JetType actualType) {
            this.expression = expression;
            this.expectedType = expectedType;
            this.actualType = actualType;
        }

        @Override
        public void acceptHandler(@NotNull ErrorHandler handler) {
            handler.typeMismatch(expression, expectedType, actualType);
        }
    }

    public static class RedeclarationError extends JetDiagnostic {

        private final DeclarationDescriptor existingDescriptor;
        private final DeclarationDescriptor redeclaredDescriptor;

        public RedeclarationError(@NotNull DeclarationDescriptor existingDescriptor, @NotNull DeclarationDescriptor redeclaredDescriptor) {
            this.existingDescriptor = existingDescriptor;
            this.redeclaredDescriptor = redeclaredDescriptor;
        }

        @Override
        public void acceptHandler(@NotNull ErrorHandler handler) {
            handler.redeclaration(existingDescriptor, redeclaredDescriptor);
        }
    }

    public static class GenericWarning extends JetDiagnostic {

        private final ASTNode node;
        private final String message;

        public GenericWarning(@NotNull ASTNode node, @NotNull String message) {
            this.message = message;
            this.node = node;
        }

        @Override
        public void acceptHandler(@NotNull ErrorHandler handler) {
            handler.genericWarning(node, message);
        }
    }

//    private final StackTraceElement[] stackTrace;
//
//    protected JetDiagnostic() {
//        stackTrace = Thread.currentThread().getStackTrace();
//    }
//
//    public StackTraceElement[] getStackTrace() {
//        return stackTrace;
//    }

    public abstract void acceptHandler(@NotNull ErrorHandler handler);
}
