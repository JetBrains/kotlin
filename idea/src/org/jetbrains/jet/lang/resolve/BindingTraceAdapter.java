package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class BindingTraceAdapter implements BindingTrace {
    private final BindingTrace originalTrace;

    public BindingTraceAdapter(BindingTrace originalTrace) {
        this.originalTrace = originalTrace;
    }

    public void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type) {
        originalTrace.recordExpressionType(expression, type);
    }

    public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
        originalTrace.recordReferenceResolution(expression, descriptor);
    }

    public void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element) {
        originalTrace.recordLabelResolution(expression, element);
    }

    public void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor) {
        originalTrace.recordDeclarationResolution(declaration, descriptor);
    }

    @Override
    public void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor) {
        originalTrace.recordValueParameterAsPropertyResolution(declaration, descriptor);
    }

    public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type) {
        originalTrace.recordTypeResolution(typeReference, type);
    }

    public void recordBlock(JetFunctionLiteralExpression expression) {
        originalTrace.recordBlock(expression);
    }

    public void removeReferenceResolution(@NotNull JetReferenceExpression referenceExpression) {
        originalTrace.removeReferenceResolution(referenceExpression);
    }

    @Override
    public void recordStatement(@NotNull JetElement statement) {
        originalTrace.recordStatement(statement);
    }

    @Override
    public void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        originalTrace.requireBackingField(propertyDescriptor);
    }

    @NotNull
    @Override
    public ErrorHandler getErrorHandler() {
        return originalTrace.getErrorHandler();
    }

    @Override
    public void removeStatementRecord(@NotNull JetElement statement) {
        originalTrace.removeStatementRecord(statement);
    }

    @Override
    public void markAsProcessed(@NotNull JetExpression expression) {
        originalTrace.markAsProcessed(expression);
    }

    @Override
    public boolean isProcessed(@NotNull JetExpression expression) {
        return originalTrace.isProcessed(expression);
    }

    @Override
    public BindingContext getBindingContext() {
        return originalTrace.getBindingContext();
    }
}
