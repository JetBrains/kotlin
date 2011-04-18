package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl {

    private PropertyAccessorDescriptor getter;
    private PropertyAccessorDescriptor setter;
    private boolean hasBackingField;

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Attribute> attributes,
            @NotNull String name,
            @Nullable JetType inType,
            @Nullable JetType outType) {
        super(containingDeclaration, attributes, name, inType, outType);
    }

    public PropertyAccessorDescriptor getGetter() {
        return getter;
    }

    public PropertyAccessorDescriptor getSetter() {
        return setter;
    }

    public boolean hasBackingFiled() {
        return hasBackingField;
    }

    @NotNull
    @Override
    public VariableDescriptor substitute(TypeSubstitutor substitutor) {
        JetType originalInType = getInType();
        JetType inType = originalInType == null ? null : substitutor.substitute(originalInType, Variance.IN_VARIANCE);
        JetType outType = substitutor.substitute(getOutType(), Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }
        return new PropertyDescriptor(
                getContainingDeclaration(),
                getAttributes(), // TODO
                getName(),
                inType,
                outType
        );
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }
}
