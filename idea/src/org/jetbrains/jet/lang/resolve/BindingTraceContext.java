package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.*;

/**
 * @author abreslav
 */
public class BindingTraceContext implements BindingContext, BindingTrace {
    private final Map<JetExpression, JetType> expressionTypes = new HashMap<JetExpression, JetType>();
    private final Map<JetReferenceExpression, DeclarationDescriptor> resolutionResults = new HashMap<JetReferenceExpression, DeclarationDescriptor>();
    private final Map<JetReferenceExpression, PsiElement> labelResolutionResults = new HashMap<JetReferenceExpression, PsiElement>();
    private final Map<JetTypeReference, JetType> types = new HashMap<JetTypeReference, JetType>();
    private final Map<DeclarationDescriptor, PsiElement> descriptorToDeclarations = new HashMap<DeclarationDescriptor, PsiElement>();
    private final Map<PsiElement, DeclarationDescriptor> declarationsToDescriptors = new HashMap<PsiElement, DeclarationDescriptor>();
    private final Map<PsiElement, ConstructorDescriptor> constructorDeclarationsToDescriptors = new HashMap<PsiElement, ConstructorDescriptor>();
    private final Map<PsiElement, NamespaceDescriptor> namespaceDeclarationsToDescriptors = Maps.newHashMap();
    private final Map<PsiElement, PropertyDescriptor> primaryConstructorParameterDeclarationsToPropertyDescriptors = Maps.newHashMap();
    private final Set<JetFunctionLiteralExpression> blocks = new HashSet<JetFunctionLiteralExpression>();
    private final Set<JetElement> statements = new HashSet<JetElement>();
    private final Set<PropertyDescriptor> backingFieldRequired = new HashSet<PropertyDescriptor>();
    private final Set<JetExpression> processed = Sets.newHashSet();

    private Collection<JetDiagnostic> diagnostics = Lists.newArrayList();
    private ErrorHandler errorHandler = new ErrorHandler() {
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
    };

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public void recordExpressionType(@NotNull JetExpression expression, @NotNull JetType type) {
        safePut(expressionTypes, expression, type);
    }

    @Override
    public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
        resolutionResults.put(expression, descriptor);
    }

    @Override
    public void recordLabelResolution(@NotNull JetReferenceExpression expression, @NotNull PsiElement element) {
        safePut(labelResolutionResults, expression, element);
    }

    @Override
    public void removeReferenceResolution(@NotNull JetReferenceExpression referenceExpression) {
        resolutionResults.remove(referenceExpression);
    }

    @Override
    public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull JetType type) {
        safePut(types, typeReference, type);
    }

    @Override
    public void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor) {
        safePut(descriptorToDeclarations, descriptor.getOriginal(), declaration);
        descriptor.accept(new DeclarationDescriptorVisitor<Void, PsiElement>() {
                    @Override
                    public Void visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, PsiElement declaration) {
                        safePut(constructorDeclarationsToDescriptors, declaration, constructorDescriptor);
                        return null;
                    }

                    @Override
                    public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, PsiElement declaration) {
                        safePut(namespaceDeclarationsToDescriptors, declaration, descriptor);
                        return null;
                    }

                    public Void visitDeclarationDescriptor(DeclarationDescriptor descriptor, PsiElement declaration) {
                        safePut(declarationsToDescriptors, declaration, descriptor.getOriginal());
                        return null;
                    }
                }, declaration);
    }

    @Override
    public void recordValueParameterAsPropertyResolution(@NotNull JetParameter declaration, @NotNull PropertyDescriptor descriptor) {
        safePut(primaryConstructorParameterDeclarationsToPropertyDescriptors, declaration, descriptor);
        safePut(descriptorToDeclarations, descriptor.getOriginal(), declaration);
    }

    private <K, V> void safePut(Map<K, V> map, K key, V value) {
        V oldValue = map.put(key, value);
        // TODO:
        assert oldValue == null || oldValue == value : key + ": " + oldValue + " and " + value;
    }

    @Override
    public void requireBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        backingFieldRequired.add(propertyDescriptor);
    }

    @Override
    public void recordBlock(JetFunctionLiteralExpression expression) {
        blocks.add(expression);
    }

    @Override
    public void recordStatement(@NotNull JetElement statement) {
        statements.add(statement);
    }

    @Override
    public void removeStatementRecord(@NotNull JetElement statement) {
        statements.remove(statement);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public DeclarationDescriptor getDeclarationDescriptor(PsiElement declaration) {
        if (declaration instanceof JetNamespace) {
            JetNamespace namespace = (JetNamespace) declaration;
            return getNamespaceDescriptor(namespace);
        }
        return declarationsToDescriptors.get(declaration);
    }

    public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
        return namespaceDeclarationsToDescriptors.get(declaration);
    }

    @Override
    public ClassDescriptor getClassDescriptor(JetClassOrObject declaration) {
        return (ClassDescriptor) declarationsToDescriptors.get((JetDeclaration) declaration);
    }

    @Override
    public TypeParameterDescriptor getTypeParameterDescriptor(JetTypeParameter declaration) {
        return (TypeParameterDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public FunctionDescriptor getFunctionDescriptor(JetFunction declaration) {
        return (FunctionDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public VariableDescriptor getVariableDescriptor(JetProperty declaration) {
        return (VariableDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public VariableDescriptor getVariableDescriptor(JetParameter declaration) {
        return (VariableDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(JetParameter primaryConstructorParameter) {
        return primaryConstructorParameterDeclarationsToPropertyDescriptors.get(primaryConstructorParameter);
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(JetObjectDeclarationName objectDeclarationName) {
        return (PropertyDescriptor) declarationsToDescriptors.get(objectDeclarationName);
    }

    @Nullable
    @Override
    public ConstructorDescriptor getConstructorDescriptor(@NotNull JetElement declaration) {
        return constructorDeclarationsToDescriptors.get(declaration);
    }

    @Override
    public JetType resolveTypeReference(JetTypeReference typeReference) {
        return types.get(typeReference);
    }

    @Override
    public JetType getExpressionType(JetExpression expression) {
        return expressionTypes.get(expression);
    }

    @Override
    public DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression) {
        return resolutionResults.get(referenceExpression);
    }

    @Override
    public PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = resolveReferenceExpression(referenceExpression);
        if (declarationDescriptor == null) {
            return labelResolutionResults.get(referenceExpression);
        }
        return descriptorToDeclarations.get(declarationDescriptor.getOriginal());
    }

    @Override
    public PsiElement getDeclarationPsiElement(@NotNull DeclarationDescriptor descriptor) {
        return descriptorToDeclarations.get(descriptor.getOriginal());
    }

    @Override
    public boolean isBlock(JetFunctionLiteralExpression expression) {
        return !expression.hasParameterSpecification() && blocks.contains(expression);
    }

    @Override
    public boolean isStatement(@NotNull JetExpression expression) {
        return statements.contains(expression);
    }

    @Override
    public boolean hasBackingField(@NotNull PropertyDescriptor propertyDescriptor) {
        PsiElement declarationPsiElement = getDeclarationPsiElement(propertyDescriptor);
        if (declarationPsiElement instanceof JetParameter) {
            JetParameter jetParameter = (JetParameter) declarationPsiElement;
            return jetParameter.getValOrVarNode() != null ||
                   backingFieldRequired.contains(propertyDescriptor);
        }
        if (propertyDescriptor.getModifiers().isAbstract()) return false;
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        if (getter == null) {
            return true;
        }
        else if (propertyDescriptor.isVar() && setter == null) {
            return true;
        }
        else if (setter != null && !setter.hasBody() && !setter.getModifiers().isAbstract()) {
            return true;
        }
        else if (!getter.hasBody() && !getter.getModifiers().isAbstract()) {
            return true;
        }
        return backingFieldRequired.contains(propertyDescriptor);
    }

    public ConstructorDescriptor resolveSuperConstructor(JetDelegatorToSuperCall superCall) {
        JetTypeReference typeReference = superCall.getTypeReference();
        if (typeReference == null) return null;

        JetTypeElement typeElement = typeReference.getTypeElement();
        if (!(typeElement instanceof JetUserType)) return null;

        DeclarationDescriptor descriptor = resolveReferenceExpression(((JetUserType) typeElement).getReferenceExpression());
        return descriptor instanceof ConstructorDescriptor ? (ConstructorDescriptor) descriptor : null;
    }

    @Override
    public Collection<JetDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    @Override
    public void markAsProcessed(@NotNull JetExpression expression) {
        processed.add(expression);
    }

    @Override
    public boolean isProcessed(@NotNull JetExpression expression) {
        return processed.contains(expression);
    }

    @Override
    public BindingContext getBindingContext() {
        return this;
    }
}
