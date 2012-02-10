package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public abstract class PropertyAccessorDescriptor extends DeclarationDescriptorImpl implements FunctionDescriptor, MemberDescriptor {

    private final boolean hasBody;
    private final boolean isDefault;
    private final Modality modality;
    private final Visibility visibility;
    private final PropertyDescriptor correspondingProperty;
    private final Kind kind;

    public PropertyAccessorDescriptor(
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            boolean hasBody,
            boolean isDefault,
            Kind kind) {
        super(correspondingProperty.getContainingDeclaration(), annotations, name);
        this.modality = modality;
        this.visibility = visibility;
        this.correspondingProperty = correspondingProperty;
        this.hasBody = hasBody;
        this.isDefault = isDefault;
        this.kind = kind;
    }

    public boolean hasBody() {
        return hasBody;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public Kind getKind() {
        return kind;
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
    public ReceiverDescriptor getReceiverParameter() {
        return getCorrespondingProperty().getReceiverParameter();
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return getCorrespondingProperty().getExpectedThisObject();
    }

    @NotNull
    @Override
    public PropertyAccessorDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        throw new UnsupportedOperationException("Accessors must be copied by the corresponding property");
    }

    protected Set<PropertyAccessorDescriptor> getOverriddenDescriptors(boolean isGetter) {
        Set<? extends PropertyDescriptor> overriddenProperties = getCorrespondingProperty().getOverriddenDescriptors();
        Set<PropertyAccessorDescriptor> overriddenAccessors = Sets.newHashSet();
        for (PropertyDescriptor overriddenProperty : overriddenProperties) {
            PropertyAccessorDescriptor accessorDescriptor = isGetter ? overriddenProperty.getGetter() : overriddenProperty.getSetter();
            if (accessorDescriptor != null) {
                overriddenAccessors.add(accessorDescriptor);
            }
        }
        return overriddenAccessors;
    }
}
