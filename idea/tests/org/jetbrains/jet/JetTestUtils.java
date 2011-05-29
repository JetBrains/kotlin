package org.jetbrains.jet;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ClassCodegen;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @author abreslav
 */
public class JetTestUtils {
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
            return new BindingContext() {

                @Override
                public DeclarationDescriptor getDeclarationDescriptor(PsiElement declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public ClassDescriptor getClassDescriptor(JetClass declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public TypeParameterDescriptor getTypeParameterDescriptor(JetTypeParameter declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public FunctionDescriptor getFunctionDescriptor(JetFunction declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public ConstructorDescriptor getConstructorDescriptor(JetElement declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public VariableDescriptor getVariableDescriptor(JetProperty declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public VariableDescriptor getVariableDescriptor(JetParameter declaration) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public PropertyDescriptor getPropertyDescriptor(JetParameter primaryConstructorParameter) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public JetType getExpressionType(JetExpression expression) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public JetType resolveTypeReference(JetTypeReference typeReference) {
                    return null;
                }

                @Override
                public PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public PsiElement getDeclarationPsiElement(DeclarationDescriptor descriptor) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public boolean isBlock(JetFunctionLiteralExpression expression) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public boolean isStatement(JetExpression expression) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public boolean hasBackingField(PropertyDescriptor propertyDescriptor) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public ConstructorDescriptor resolveSuperConstructor(JetDelegatorToSuperCall superCall) {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public Collection<JetDiagnostic> getDiagnostics() {
                    throw new UnsupportedOperationException(); // TODO
                }

            };
        }
    };
}
