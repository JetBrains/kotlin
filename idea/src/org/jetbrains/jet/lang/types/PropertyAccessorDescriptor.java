package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class PropertyAccessorDescriptor extends DeclarationDescriptorImpl implements FunctionDescriptor {

    protected PropertyAccessorDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Attribute> attributes, @NotNull String name) {
        super(correspondingProperty.getContainingDeclaration(), attributes, name);
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
}
