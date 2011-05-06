package org.jetbrains.jet.lang.types;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.JetScope;

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
        public void setToplevelScope(JetScope toplevelScope) {
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
    };

    public void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type);

    public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor);

    public void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element);

    public void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor);

    public void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor);

    public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type);

    public void setToplevelScope(JetScope toplevelScope);

    public void recordBlock(JetFunctionLiteralExpression expression);

    public void recordStatement(@NotNull JetElement statement);

    public void removeStatementRecord(@NotNull JetElement statement);

    public void removeReferenceResolution(@NotNull JetReferenceExpression referenceExpression);

    public void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor);

    @NotNull
    public ErrorHandler getErrorHandler();
}
