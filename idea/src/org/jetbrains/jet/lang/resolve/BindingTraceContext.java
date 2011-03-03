package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author abreslav
 */
public class BindingTraceContext extends BindingTrace implements BindingContext {
    private final Map<JetExpression, Type> expressionTypes = new HashMap<JetExpression, Type>();
    private final Map<JetReferenceExpression, DeclarationDescriptor> resolutionResults = new HashMap<JetReferenceExpression, DeclarationDescriptor>();
    private final Map<JetTypeReference, Type> types = new HashMap<JetTypeReference, Type>();
    private final Map<DeclarationDescriptor, PsiElement> descriptorToDeclarations = new HashMap<DeclarationDescriptor, PsiElement>();
    private final Map<PsiElement, DeclarationDescriptor> declarationsToDescriptors = new HashMap<PsiElement, DeclarationDescriptor>();
    private JetScope toplevelScope;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void recordExpressionType(@NotNull JetExpression expression, @NotNull Type type) {
        expressionTypes.put(expression, type);
    }

    @Override
    public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
        resolutionResults.put(expression, descriptor);
    }

    @Override
    public void recordTypeResolution(@NotNull JetTypeReference typeReference, @NotNull Type type) {
        types.put(typeReference, type);
    }

    @Override
    public void recordDeclarationResolution(@NotNull PsiElement declaration, @NotNull DeclarationDescriptor descriptor) {
        descriptorToDeclarations.put(descriptor, declaration);
        declarationsToDescriptors.put(declaration, descriptor);
    }

    public void setToplevelScope(JetScope toplevelScope) {
        this.toplevelScope = toplevelScope;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
        return (NamespaceDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public ClassDescriptor getClassDescriptor(JetClass declaration) {
        return (ClassDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public FunctionDescriptor getFunctionDescriptor(JetFunction declaration) {
        return (FunctionDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(JetProperty declaration) {
        return (PropertyDescriptor) declarationsToDescriptors.get(declaration);
    }

    @Override
    public Type resolveTypeReference(JetTypeReference typeReference) {
        return types.get(typeReference);
    }

    @Override
    public Type getExpressionType(JetExpression expression) {
        return expressionTypes.get(expression);
    }

    @Override
    public DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression) {
        return resolutionResults.get(referenceExpression);
    }

    @Override
    public JetScope getTopLevelScope() {
        return toplevelScope;
    }

    @Override
    public PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression) {
        return descriptorToDeclarations.get(resolveReferenceExpression(referenceExpression));
    }

}
