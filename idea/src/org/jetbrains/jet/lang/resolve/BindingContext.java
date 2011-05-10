package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.ClassCodegen;
import org.jetbrains.jet.lang.JetDiagnostic;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface BindingContext {
    @Deprecated // "Tests only"
    DeclarationDescriptor getDeclarationDescriptor(PsiElement declaration);

    NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration);
    ClassDescriptor getClassDescriptor(JetClass declaration);
    TypeParameterDescriptor getTypeParameterDescriptor(JetTypeParameter declaration);
    FunctionDescriptor getFunctionDescriptor(JetFunction declaration);
    ConstructorDescriptor getConstructorDescriptor(JetElement declaration);

    VariableDescriptor getVariableDescriptor(JetProperty declaration);
    VariableDescriptor getVariableDescriptor(JetParameter declaration);

    PropertyDescriptor getPropertyDescriptor(JetParameter primaryConstructorParameter);

    JetType getExpressionType(JetExpression expression);

    DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression);

    JetType resolveTypeReference(JetTypeReference typeReference);
    PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression);
    PsiElement getDeclarationPsiElement(DeclarationDescriptor descriptor);

    boolean isBlock(JetFunctionLiteralExpression expression);
    boolean isStatement(JetExpression expression);
    boolean hasBackingField(PropertyDescriptor propertyDescriptor);

    ConstructorDescriptor resolveSuperConstructor(JetDelegatorToSuperCall superCall, ClassCodegen classCodegen);

    Collection<JetDiagnostic> getDiagnostics();
}
