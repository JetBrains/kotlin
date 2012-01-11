package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.List;

/**
 * @author abreslav
 */
public class NamespaceDescriptorImpl extends AbstractNamespaceDescriptorImpl implements NamespaceLike {

    private WritableScope memberScope;

    public NamespaceDescriptorImpl(@Nullable DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    public void initialize(@NotNull WritableScope memberScope) {
        this.memberScope = memberScope;
    }

    @Override
    @NotNull
    public WritableScope getMemberScope() {
        return memberScope;
    }

    @Override
    public NamespaceDescriptorImpl getNamespace(String name) {
        return (NamespaceDescriptorImpl) memberScope.getDeclaredNamespace(name);
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        memberScope.addNamespace(namespaceDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {
        memberScope.addClassifierDescriptor(classDescriptor);
    }

    @Override
    public void addObjectDescriptor(@NotNull MutableClassDescriptor objectDescriptor) {
        memberScope.addObjectDescriptor(objectDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        memberScope.addFunctionDescriptor(functionDescriptor);
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        memberScope.addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
        throw new IllegalStateException("Must be guaranteed not to happen by the parser");
    }
}
