package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class WritableBindingContext implements BindingContext {
    @Override
    public NamespaceDescriptor getNamespaceDescriptor(JetNamespace declaration) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassDescriptor getClassDescriptor(JetClass declaration) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public FunctionDescriptor getFunctionDescriptor(JetFunction declaration) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(JetProperty declaration) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Type getExpressionType(JetExpression expression) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public DeclarationDescriptor resolve(JetReferenceExpression referenceExpression) {
        throw new UnsupportedOperationException(); // TODO
    }
}
