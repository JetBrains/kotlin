package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class BindingContext {
    public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
        return null;
    }

    public ClassDescriptor getClassDescriptor(JetClass declaration) {
        return null;
    }

    public FunctionDescriptor getFunctionDescriptor(JetFunction declaration) {
        return null;
    }

    public PropertyDescriptor getPropertyDescriptor(JetProperty declaration) {
        return null;
    }


    public Type getExpressionType(JetExpression expression) {
        return null;
    }

    public JetScope getTopLevelScope() {
        return null;
    }


    public DeclarationDescriptor resolveReferenceExpression(JetReferenceExpression referenceExpression) {
        return null;
    }

    public Type resolveTypeReference(JetTypeReference typeReference) {
        return null;
    }


    public PsiElement resolveToDeclarationPsiElement(JetReferenceExpression referenceExpression) {
        return null;
    }

}
