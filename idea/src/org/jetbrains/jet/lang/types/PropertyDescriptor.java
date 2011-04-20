package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements MemberDescriptor {

    private final MemberModifiers memberModifiers;
    private final boolean isVar;
    private PropertyGetterDescriptor getter;
    private PropertySetterDescriptor setter;

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Attribute> attributes,
            @NotNull MemberModifiers memberModifiers,
            boolean isVar,
            @NotNull String name,
            @Nullable JetType inType,
            @Nullable JetType outType) {
        super(containingDeclaration, attributes, name, inType, outType);
        assert !isVar || inType != null;
        assert outType != null;
        this.isVar = isVar;
        this.memberModifiers = memberModifiers;
    }

    public PropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @Nullable JetType inType,
            @Nullable JetType outType) {
        this(
                original.getContainingDeclaration(),
                original.getAttributes(), // TODO : substitute?
                original.getModifiers(),
                original.isVar,
                original.getName(),
                inType,
                outType);
    }

    public void initialize(@Nullable PropertyGetterDescriptor getter, @Nullable PropertySetterDescriptor setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public boolean isVar() {
        return isVar;
    }

    @NotNull
    @Override
    public MemberModifiers getModifiers() {
        return memberModifiers;
    }

    @Nullable
    public PropertyGetterDescriptor getGetter() {
        return getter;
    }

    @Nullable
    public PropertySetterDescriptor getSetter() {
        return setter;
    }

    @NotNull
    @Override
    public VariableDescriptor substitute(TypeSubstitutor substitutor) {
        JetType originalInType = getInType();
        JetType inType = originalInType == null ? null : substitutor.substitute(originalInType, Variance.IN_VARIANCE);
        JetType originalOutType = getOutType();
        JetType outType = originalOutType == null ? null : substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }
        return new PropertyDescriptor(
                this,
                inType,
                outType
        );
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }
}
