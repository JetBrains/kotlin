package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class PropertyAccessorDescriptor extends DeclarationDescriptorImpl implements FunctionDescriptor {

    private final boolean hasBody;

    protected PropertyAccessorDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Annotation> annotations, @NotNull String name, boolean hasBody) {
        super(correspondingProperty.getContainingDeclaration(), annotations, name);
        this.hasBody = hasBody;
    }

    public boolean hasBody() {
        return hasBody;
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
