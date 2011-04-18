package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class PropertyAccessorDescriptor extends DeclarationDescriptorImpl implements FunctionDescriptor {
    public enum AccessorType {
        GETTER("get"),
        SETTER("set");

        private final String name;

        AccessorType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static PropertyAccessorDescriptor createGetterDescriptor(
        @NotNull PropertyDescriptor correspondingProperty,
        @NotNull List<Attribute> attributes,
        @NotNull JetType returnType
    ) {
        return new PropertyAccessorDescriptor(correspondingProperty, attributes, AccessorType.GETTER);
    }

    public static PropertyAccessorDescriptor createSetterDescriptor(
        @NotNull PropertyDescriptor correspondingProperty,
        @NotNull List<Attribute> attributes,
        @NotNull ValueParameterDescriptor parameter
    ) {
        return new PropertyAccessorDescriptor(correspondingProperty, attributes, AccessorType.SETTER);
    }

    protected PropertyAccessorDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Attribute> attributes, @NotNull AccessorType accessorType) {
        super(correspondingProperty.getContainingDeclaration(), attributes, accessorType.getName());
    }

    @NotNull
    @Override
    public PropertyAccessorDescriptor getOriginal() {
        return (PropertyAccessorDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    public FunctionDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public JetType getUnsubstitutedReturnType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        throw new UnsupportedOperationException(); // TODO
    }
}
