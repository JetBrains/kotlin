package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public PropertyGetterDescriptor(@NotNull MemberModifiers modifiers, @NotNull PropertyDescriptor correspondingProperty, @NotNull List<Annotation> annotations, @Nullable JetType returnType, boolean hasBody) {
        super(modifiers, correspondingProperty, annotations, "get-" + correspondingProperty.getName(), hasBody);
        this.returnType = returnType == null ? correspondingProperty.getOutType() : returnType;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenFunctions() {
        return overriddenGetters;
    }

    public void addOverriddenFunction(@NotNull PropertyGetterDescriptor overriddenGetter) {
        overriddenGetters.add(overriddenGetter);
    }

    @Override
    public JetType getReceiverType() {
        return null; // TODO
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
