package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class PropertyAccessorDescriptor extends DeclarationDescriptorImpl implements FunctionDescriptor, MemberDescriptor {

    private final boolean hasBody;
    private final boolean isDefault;
    private final Modality modality;
    private final PropertyDescriptor correspondingProperty;

    protected PropertyAccessorDescriptor(
            @NotNull Modality modality,
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            boolean hasBody,
            boolean isDefault) {
        super(correspondingProperty.getContainingDeclaration(), annotations, name);
        this.correspondingProperty = correspondingProperty;
        this.modality = modality;
        this.hasBody = hasBody;
        this.isDefault = isDefault;
    }

    public boolean hasBody() {
        return hasBody;
    }

    public boolean isDefault() {
        return isDefault;
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
    public Modality getModality() {
        return modality;
    }

    @NotNull
    public PropertyDescriptor getCorrespondingProperty() {
        return correspondingProperty;
    }
}
