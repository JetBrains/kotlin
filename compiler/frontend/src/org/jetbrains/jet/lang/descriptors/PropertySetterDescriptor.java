package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class PropertySetterDescriptor extends PropertyAccessorDescriptor {

    private ValueParameterDescriptor parameter;

    public PropertySetterDescriptor(
            @NotNull PropertyDescriptor correspondingProperty,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean hasBody,
            boolean isDefault,
            Kind kind) {
        super(modality, visibility, correspondingProperty, annotations, "set-" + correspondingProperty.getName(), hasBody, isDefault, kind);
    }

    public void initialize(@NotNull ValueParameterDescriptor parameter) {
        assert this.parameter == null;
        this.parameter = parameter;
    }

    public void initializeDefault() {
        assert parameter == null;
        parameter = new ValueParameterDescriptorImpl(this, 0, Collections.<AnnotationDescriptor>emptyList(), "<>", false, getCorrespondingProperty().getReturnType(), false, null);
    }

    @NotNull
    @Override
    public Set<? extends PropertyAccessorDescriptor> getOverriddenDescriptors() {
        return super.getOverriddenDescriptors(false);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        if (parameter == null) {
            throw new IllegalStateException();
        }
        return Collections.singletonList(parameter);
    }

    @NotNull
    @Override
    public JetType getReturnType() {
        return JetStandardClasses.getUnitType();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertySetterDescriptor(this, data);
    }
}
