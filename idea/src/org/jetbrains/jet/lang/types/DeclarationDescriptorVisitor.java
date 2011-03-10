package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.resolve.LazySubstitutingClassDescriptor;

/**
 * @author abreslav
 */
public class DeclarationDescriptorVisitor<R, D> {
    public R visitPropertyDescriptor(PropertyDescriptor descriptor, D data) {
        return null;
    }

    public R visitFunctionDescriptor(FunctionDescriptor descriptor, D data) {
        return null;
    }

    public R visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data) {
        return null;
    }

    public R visitNamespaceDescriptor(NamespaceDescriptor namespaceDescriptor, D data) {
        return null;
    }

    public R visitClassDescriptor(ClassDescriptor descriptor, D data) {
        return null;
    }
}
