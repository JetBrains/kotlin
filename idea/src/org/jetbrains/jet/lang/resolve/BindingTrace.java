package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.ErrorHandlerWithRegions;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface BindingTrace {

    void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type);

    void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor);

    void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element);

    void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor);

    void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor);

    void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type);

    void recordBlock(JetFunctionLiteralExpression expression);

    void recordStatement(@NotNull JetElement statement);

    void recordResolutionScope(@NotNull JetExpression expression, @NotNull JetScope scope);

    void removeStatementRecord(@NotNull JetElement statement);

    void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor);

    void recordAutoCast(@NotNull JetExpression expression, @NotNull JetType type);

    @NotNull
    ErrorHandlerWithRegions getErrorHandler();

    boolean isProcessed(@NotNull JetExpression expression);

    void markAsProcessed(@NotNull JetExpression expression);

    BindingContext getBindingContext();
}
