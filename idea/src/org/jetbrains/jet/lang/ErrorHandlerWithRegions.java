package org.jetbrains.jet.lang;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Stack;

/**
 * @author abreslav
 */
public class ErrorHandlerWithRegions extends ErrorHandler {

    public class DiagnosticsRegion {
        private final CollectingErrorHandler errorHandler;
        private boolean committed = false;

        private DiagnosticsRegion(CollectingErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
        }

        public CollectingErrorHandler getErrorHandler() {
            assert !committed;
            return errorHandler;
        }

        public void commit() {
            assert !committed;
            AnalyzingUtils.applyHandler(parent, errorHandler.getDiagnostics());
            committed = true;
        }
    }

    private final ErrorHandler parent;
    private final Stack<CollectingErrorHandler> workers;
    private ErrorHandler worker;

    public ErrorHandlerWithRegions(ErrorHandler parent) {
        this.parent = parent;
        this.worker = parent;
        this.workers = new Stack<CollectingErrorHandler>();
    }

    public void openRegion() {
        CollectingErrorHandler newWorker = new CollectingErrorHandler();
        workers.push(newWorker);
        worker = newWorker;
    }

    public void closeAndCommitCurrentRegion() {
        assert !workers.isEmpty();
        CollectingErrorHandler region = workers.pop();
        AnalyzingUtils.applyHandler(parent, region.getDiagnostics());

        setWorker();
    }

    public DiagnosticsRegion closeAndReturnCurrentRegion() {
        assert !workers.isEmpty();
        CollectingErrorHandler currentWorker = workers.pop();
        setWorker();
        return new DiagnosticsRegion(currentWorker);
    }

    private void setWorker() {
        worker = workers.isEmpty() ? parent : workers.peek();
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
