package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @author abreslav
 */
public class ChildBindingTrace extends BindingTraceContext {

    private final BindingContext parentBindingContext;

    private final BindingContext bindingContext = new BindingContext() {
        @Override
        @Deprecated
        public DeclarationDescriptor getDeclarationDescriptor(PsiElement declaration) {
            DeclarationDescriptor value = ChildBindingTrace.super.getBindingContext().getDeclarationDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getDeclarationDescriptor(declaration);
        }

        @Override
        public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
            NamespaceDescriptor value = ChildBindingTrace.super.getBindingContext().getNamespaceDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getNamespaceDescriptor(declaration);
        }

        @Override
        public ClassDescriptor getClassDescriptor(JetClassOrObject declaration) {
            ClassDescriptor value = ChildBindingTrace.super.getBindingContext().getClassDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getClassDescriptor(declaration);
        }

        @Override
        public TypeParameterDescriptor getTypeParameterDescriptor(JetTypeParameter declaration) {
            TypeParameterDescriptor value = ChildBindingTrace.super.getBindingContext().getTypeParameterDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getTypeParameterDescriptor(declaration);
        }

        @Override
        public FunctionDescriptor getFunctionDescriptor(JetNamedFunction declaration) {
            FunctionDescriptor value = ChildBindingTrace.super.getBindingContext().getFunctionDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getFunctionDescriptor(declaration);
        }

        @Override
        public ConstructorDescriptor getConstructorDescriptor(JetElement declaration) {
            ConstructorDescriptor value = ChildBindingTrace.super.getBindingContext().getConstructorDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getConstructorDescriptor(declaration);
        }

        @Override
        public AnnotationDescriptor getAnnotationDescriptor(JetAnnotationEntry annotationEntry) {
            AnnotationDescriptor value = ChildBindingTrace.super.getBindingContext().getAnnotationDescriptor(annotationEntry);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getAnnotationDescriptor(annotationEntry);
        }

        @Override
        @Nullable
        public CompileTimeConstant<?> getCompileTimeValue(JetExpression expression) {
            return parentBindingContext.getCompileTimeValue(expression);
        }

        @Override
        public VariableDescriptor getVariableDescriptor(JetProperty declaration) {
            VariableDescriptor value = ChildBindingTrace.super.getBindingContext().getVariableDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getVariableDescriptor(declaration);
        }

        @Override
        public VariableDescriptor getVariableDescriptor(JetParameter declaration) {
            VariableDescriptor value = ChildBindingTrace.super.getBindingContext().getVariableDescriptor(declaration);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getVariableDescriptor(declaration);
        }

        @Override
        public PropertyDescriptor getPropertyDescriptor(JetParameter primaryConstructorParameter) {
            PropertyDescriptor value = ChildBindingTrace.super.getBindingContext().getPropertyDescriptor(primaryConstructorParameter);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getPropertyDescriptor(primaryConstructorParameter);
        }

        @Override
        public PropertyDescriptor getPropertyDescriptor(JetObjectDeclarationName objectDeclarationName) {
            PropertyDescriptor value = ChildBindingTrace.super.getBindingContext().getPropertyDescriptor(objectDeclarationName);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getPropertyDescriptor(objectDeclarationName);
        }

        @Override
        public JetType getExpressionType(JetExpression expression) {
            JetType value = ChildBindingTrace.super.getBindingContext().getExpressionType(expression);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getExpressionType(expression);
        }

        @Override
        public DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression) {
            DeclarationDescriptor value = ChildBindingTrace.super.getBindingContext().resolveReferenceExpression(referenceExpression);
            if (value != null) {
                return value;
            }
            return parentBindingContext.resolveReferenceExpression(referenceExpression);
        }

        @Override
        public JetType resolveTypeReference(JetTypeReference typeReference) {
            JetType value = ChildBindingTrace.super.getBindingContext().resolveTypeReference(typeReference);
            if (value != null) {
                return value;
            }
            return parentBindingContext.resolveTypeReference(typeReference);
        }

        @Override
        public PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression) {
            PsiElement value = ChildBindingTrace.super.getBindingContext().resolveToDeclarationPsiElement(referenceExpression);
            if (value != null) {
                return value;
            }
            return parentBindingContext.resolveToDeclarationPsiElement(referenceExpression);
        }

        @Override
        public PsiElement getDeclarationPsiElement(DeclarationDescriptor descriptor) {
            PsiElement value = ChildBindingTrace.super.getBindingContext().getDeclarationPsiElement(descriptor);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getDeclarationPsiElement(descriptor);
        }

        @Override
        public boolean isBlock(JetFunctionLiteralExpression expression) {
            boolean value = ChildBindingTrace.super.getBindingContext().isBlock(expression);
            if (!value) {
                return value;
            }
            return parentBindingContext.isBlock(expression);
        }

        @Override
        public boolean isStatement(JetExpression expression) {
            boolean value = ChildBindingTrace.super.getBindingContext().isStatement(expression);
            if (!value) {
                return value;
            }
            return parentBindingContext.isStatement(expression);
        }

        @Override
        public boolean hasBackingField(PropertyDescriptor propertyDescriptor) {
            boolean value = ChildBindingTrace.super.getBindingContext().hasBackingField(propertyDescriptor);
            if (!value) {
                return value;
            }
            return parentBindingContext.hasBackingField(propertyDescriptor);
        }

        @Override
        public boolean isVariableReassignment(JetExpression expression) {
            boolean value = ChildBindingTrace.super.getBindingContext().isVariableReassignment(expression);
            if (!value) {
                return value;
            }
            return parentBindingContext.isVariableReassignment(expression);
        }

        @Override
        public ConstructorDescriptor resolveSuperConstructor(JetDelegatorToSuperCall superCall) {
            ConstructorDescriptor value = ChildBindingTrace.super.getBindingContext().resolveSuperConstructor(superCall);
            if (value != null) {
                return value;
            }
            return parentBindingContext.resolveSuperConstructor(superCall);
        }

        @Override
        @Nullable
        public JetType getAutoCastType(@NotNull JetExpression expression) {
            JetType value = ChildBindingTrace.super.getBindingContext().getAutoCastType(expression);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getAutoCastType(expression);
        }

        @Override
        @Nullable
        public JetScope getResolutionScope(@NotNull JetExpression expression) {
            JetScope value = ChildBindingTrace.super.getBindingContext().getResolutionScope(expression);
            if (value != null) {
                return value;
            }
            return parentBindingContext.getResolutionScope(expression);
        }

        @Override
        public Collection<JetDiagnostic> getDiagnostics() {
            // This deliberately returns only my own diagnostics
            return ChildBindingTrace.super.getBindingContext().getDiagnostics();
        }
    };

    public ChildBindingTrace(BindingContext parent) {
        this.parentBindingContext = parent;
    }

    @Override
    public BindingContext getBindingContext() {
        return parentBindingContext;
    }
}
