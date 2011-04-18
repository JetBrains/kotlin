package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class PropertySetterDescriptor extends PropertyAccessorDescriptor implements MutableFunctionDescriptor {

    private MutableValueParameterDescriptor parameter;

    public PropertySetterDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Attribute> attributes) {
        super(correspondingProperty, attributes, "set-" + correspondingProperty.getName());
    }

    public void initialize(@NotNull MutableValueParameterDescriptor parameter) {
        assert this.parameter == null;
        this.parameter = parameter;
    }

    public void setParameterType(@NotNull JetType type) {
        parameter.setType(type);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        return Collections.<ValueParameterDescriptor>singletonList(parameter);
    }

    @NotNull
    @Override
    public JetType getUnsubstitutedReturnType() {
        return JetStandardClasses.getUnitType();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertySetterDescriptor(this, data);
    }

    @Override
    public void setUnsubstitutedReturnType(@NotNull JetType type) {
        throw new UnsupportedOperationException("Can't set return type for a setter");
    }

    @Override
    public boolean isReturnTypeSet() {
        return true;
    }
}
