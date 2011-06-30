package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class PropertySetterDescriptor extends PropertyAccessorDescriptor {

    private MutableValueParameterDescriptor parameter;
    private final Set<PropertySetterDescriptor> overriddenSetters = Sets.newHashSet();

    public PropertySetterDescriptor(@NotNull MemberModifiers modifiers, @NotNull PropertyDescriptor correspondingProperty, @NotNull List<Annotation> annotations, boolean hasBody) {
        super(modifiers, correspondingProperty, annotations, "set-" + correspondingProperty.getName(), hasBody);
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

    @Override
    public JetType getReceiverType() {
        return null; // TODO
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.<ValueParameterDescriptor>singletonList(parameter);
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
