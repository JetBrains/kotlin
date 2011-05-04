package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class PropertyGetterDescriptor extends PropertyAccessorDescriptor implements MutableFunctionDescriptor {
    private JetType returnType;

    public PropertyGetterDescriptor(@NotNull PropertyDescriptor correspondingProperty, @NotNull List<Annotation> annotations, @Nullable JetType returnType, boolean hasBody) {
        super(correspondingProperty, annotations, "get-" + correspondingProperty.getName(), hasBody);
        this.returnType = returnType == null ? correspondingProperty.getOutType() : returnType;
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public JetType getUnsubstitutedReturnType() {
        return returnType;
    }

    @Override
    public void setUnsubstitutedReturnType(@NotNull JetType type) {
        assert this.returnType == null : this.returnType;
        this.returnType = type;
    }

    @Override
    public boolean isReturnTypeSet() {
        return returnType != null;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyGetterDescriptor(this, data);
    }
}
