package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class DeclarationDescriptorVisitor<R, D> {
    public R visitDeclarationDescriptor(DeclarationDescriptor descriptor, D data) {
        return null;
    }

    public R visitVariableDescriptor(VariableDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitFunctionDescriptor(FunctionDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitNamespaceDescriptor(NamespaceDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitClassDescriptor(ClassDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitModuleDeclaration(ModuleDescriptor descriptor, D data) {
        return visitDeclarationDescriptor(descriptor, data);
    }

    public R visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, D data) {
        return visitFunctionDescriptor(constructorDescriptor, data);
    }

    public R visitLocalVariableDescriptor(LocalVariableDescriptor descriptor, D data) {
        return visitVariableDescriptor(descriptor, data);
    }

    public R visitPropertyDescriptor(PropertyDescriptor descriptor, D data) {
        return visitVariableDescriptor(descriptor, data);
    }

    public R visitValueParameterDescriptor(ValueParameterDescriptor descriptor, D data) {
        return visitVariableDescriptor(descriptor, data);
    }

    public R visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, D data) {
        return visitPropertyAccessorDescriptor(descriptor, data);
    }

    private R visitPropertyAccessorDescriptor(PropertyAccessorDescriptor descriptor, D data) {
        return visitFunctionDescriptor(descriptor, data);
    }

    public R visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, D data) {
        return visitPropertyAccessorDescriptor(descriptor, data);
    }
}
