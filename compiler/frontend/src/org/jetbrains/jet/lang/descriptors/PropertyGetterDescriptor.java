package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class PropertyGetterDescriptor extends PropertyAccessorDescriptor {
    private final Set<PropertyGetterDescriptor> overriddenGetters = Sets.newHashSet();
    private JetType returnType;

    public PropertyGetterDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @Nullable JetType returnType, boolean hasBody, boolean isDefault) {
        super(modality, visibility, correspondingProperty, annotations, "get-" + correspondingProperty.getName(), hasBody, isDefault);
        this.returnType = returnType == null ? correspondingProperty.getOutType() : returnType;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return overriddenGetters;
    }

    public void addOverriddenFunction(@NotNull PropertyGetterDescriptor overriddenGetter) {
        overriddenGetters.add(overriddenGetter);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public JetType getReturnType() {
        return returnType;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyGetterDescriptor(this, data);
    }
}
