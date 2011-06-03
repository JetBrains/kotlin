package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public interface NamespaceLike extends DeclarationDescriptor {

    abstract class Adapter implements NamespaceLike {
        private final DeclarationDescriptor descriptor;

        public Adapter(DeclarationDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        @NotNull
        public DeclarationDescriptor getOriginal() {
            return descriptor.getOriginal();
        }

        @Override
        @Nullable
        public DeclarationDescriptor getContainingDeclaration() {
            return descriptor.getContainingDeclaration();
        }

        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            return descriptor.substitute(substitutor);
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return descriptor.accept(visitor, data);
        }

        @Override
        public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
            descriptor.acceptVoid(visitor);
        }

        @Override
        public List<Annotation> getAnnotations() {
            return descriptor.getAnnotations();
        }

        @Override
        @NotNull
        public String getName() {
            return descriptor.getName();
        }
    }

    @Nullable
    NamespaceDescriptorImpl getNamespace(String name);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor);

    void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor);

    void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor);

    void setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor);
}
