package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface BindingContext {
    NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration);
    ClassDescriptor getClassDescriptor(JetClass declaration);
    TypeParameterDescriptor getTypeParameterDescriptor(JetTypeParameter declaration);
    FunctionDescriptor getFunctionDescriptor(JetFunction declaration);
    PropertyDescriptor getPropertyDescriptor(JetProperty declaration);
    DeclarationDescriptor getParameterDescriptor(JetParameter declaration);

    JetType getExpressionType(JetExpression expression);

    JetScope getTopLevelScope();

    DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression);

    JetType resolveTypeReference(JetTypeReference typeReference);
    PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression);
    PsiElement getDeclarationPsiElement(DeclarationDescriptor descriptor);

    boolean isBlock(JetFunctionLiteralExpression expression);
    boolean isStatement(JetExpression expression);
}
