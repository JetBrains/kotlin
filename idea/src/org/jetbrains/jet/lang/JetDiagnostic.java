package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NonNls;
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

        public UnresolvedReferenceError(@NonNls JetReferenceExpression referenceExpression) {
            this.referenceExpression = referenceExpression;
        }

        @Override
        public void acceptHandler(@NonNls ErrorHandler handler) {
            handler.unresolvedReference(referenceExpression);
        }
    }

    public static class GenericError extends JetDiagnostic {

        private final ASTNode node;
        private final String message;

        public GenericError(@NonNls ASTNode node, @NonNls String message) {
            this.node = node;
            this.message = message;
        }

        @Override
        public void acceptHandler(@NonNls ErrorHandler handler) {
            handler.genericError(node, message);
        }
    }

    public static class TypeMismatchError extends JetDiagnostic {

        private final JetExpression expression;
        private final JetType expectedType;
        private final JetType actualType;

        public TypeMismatchError(@NonNls JetExpression expression, @NonNls JetType expectedType, @NonNls JetType actualType) {
            this.expression = expression;
            this.expectedType = expectedType;
            this.actualType = actualType;
        }

        @Override
        public void acceptHandler(@NonNls ErrorHandler handler) {
            handler.typeMismatch(expression, expectedType, actualType);
        }
    }

    public static class RedeclarationError extends JetDiagnostic {

        private final DeclarationDescriptor existingDescriptor;
        private final DeclarationDescriptor redeclaredDescriptor;

        public RedeclarationError(@NonNls DeclarationDescriptor existingDescriptor, @NonNls DeclarationDescriptor redeclaredDescriptor) {
            this.existingDescriptor = existingDescriptor;
            this.redeclaredDescriptor = redeclaredDescriptor;
        }

        @Override
        public void acceptHandler(@NonNls ErrorHandler handler) {
            handler.redeclaration(existingDescriptor, redeclaredDescriptor);
        }
    }

    public static class GenericWarning extends JetDiagnostic {

        private final ASTNode node;
        private final String message;

        public GenericWarning(@NonNls ASTNode node, @NonNls String message) {
            this.message = message;
            this.node = node;
        }

        @Override
        public void acceptHandler(@NonNls ErrorHandler handler) {
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

    public abstract void acceptHandler(@NonNls ErrorHandler handler);
}
