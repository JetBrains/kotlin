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
public interface BindingTrace {
    public static final BindingTrace DUMMY = new BindingTrace() {

        @Override
        public void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type) {
        }

        @Override
        public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
        }

        @Override
        public void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element) {
        }

        @Override
        public void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor) {
        }

        @Override
        public void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor) {

        }

        @Override
        public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type) {
        }

        @Override
        public void recordBlock(JetFunctionLiteralExpression expression) {
        }

        @Override
        public void recordStatement(@NotNull JetElement statement) {
        }

        @Override
        public void removeStatementRecord(@NotNull JetElement statement) {
        }

        @Override
        public void removeReferenceResolution(@NotNull JetReferenceExpression referenceExpression) {
        }

        @Override
        public void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        }

        @NotNull
        @Override
        public ErrorHandler getErrorHandler() {
            return ErrorHandler.DO_NOTHING;
        }

        @Override
        public boolean isProcessed(@NotNull JetExpression expression) {
            return false;
        }

        @Override
        public void markAsProcessed(@NotNull JetExpression expression) {

        }

        @Override
        public BindingContext getBindingContext() {
            throw new UnsupportedOperationException();
        }
    };

    void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type);

    void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor);

    void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element);

    void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor);

    void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor);

    void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type);

    void recordBlock(JetFunctionLiteralExpression expression);

    void recordStatement(@NotNull JetElement statement);

    void removeStatementRecord(@NotNull JetElement statement);

    void removeReferenceResolution(@NotNull JetReferenceExpression referenceExpression);

    void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor);

    @NotNull
    ErrorHandler getErrorHandler();

    boolean isProcessed(@NotNull JetExpression expression);

    void markAsProcessed(@NotNull JetExpression expression);

    BindingContext getBindingContext();
}
