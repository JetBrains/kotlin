package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface BindingContext {
    NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration);
    ClassDescriptor getClassDescriptor(JetClass declaration);
    FunctionDescriptor getFunctionDescriptor(JetFunction declaration);
    PropertyDescriptor getPropertyDescriptor(JetProperty declaration);

    Type getExpressionType(JetExpression expression);
    DeclarationDescriptor resolve(JetReferenceExpression referenceExpression);

    JetScope getTopLevelScope();
}
