package org.jetbrains.jet.lang.types;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class PropertySetterDescriptor extends PropertyAccessorDescriptor implements MutableFunctionDescriptor {

    private MutableValueParameterDescriptor parameter;
    private final Set<PropertySetterDescriptor> overriddenSetters = Sets.newHashSet();

    public PropertySetterDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Annotation> annotations, boolean hasBody) {
        super(correspondingProperty, annotations, "set-" + correspondingProperty.getName(), hasBody);
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
    public Set<? extends FunctionDescriptor> getOverriddenFunctions() {
        return overriddenSetters;
    }

    public void setOverriddenFunction(@NotNull PropertySetterDescriptor overriddenSetter) {
        overriddenSetters.add(overriddenSetter);
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
