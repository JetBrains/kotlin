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
    private final Visibility visibility;
    private final PropertyDescriptor correspondingProperty;

    public PropertyAccessorDescriptor(
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            boolean hasBody,
            boolean isDefault) {
        super(correspondingProperty.getContainingDeclaration(), annotations, name);
        this.modality = modality;
        this.visibility = visibility;
        this.correspondingProperty = correspondingProperty;
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
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @NotNull
    public PropertyDescriptor getCorrespondingProperty() {
        return correspondingProperty;
    }

    @NotNull
    @Override
    public PropertyAccessorDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        throw new UnsupportedOperationException("Accessors must be copied by the corresponding property");
    }
}
