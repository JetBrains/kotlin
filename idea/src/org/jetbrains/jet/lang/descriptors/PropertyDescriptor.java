package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements MemberDescriptor {

    private final MemberModifiers memberModifiers;
    private final boolean isVar;
    private final JetType receiverType;
    private final List<TypeParameterDescriptor> typeParemeters = Lists.newArrayListWithCapacity(0);
    private final PropertyDescriptor original;
    private PropertyGetterDescriptor getter;
    private PropertySetterDescriptor setter;

    private PropertyDescriptor(
            @Nullable PropertyDescriptor original,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull MemberModifiers memberModifiers,
            boolean isVar,
            @Nullable JetType receiverType,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType) {
        super(containingDeclaration, annotations, name, inType, outType);
        assert !isVar || inType != null;
//        assert outType != null;
        this.isVar = isVar;
        this.memberModifiers = memberModifiers;
        this.receiverType = receiverType;
        this.original = original == null ? this : original;
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull MemberModifiers memberModifiers,
            boolean isVar,
            @Nullable JetType receiverType,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType) {
        this(null, containingDeclaration, annotations, memberModifiers, isVar, receiverType, name, inType, outType);
    }

    private PropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @Nullable JetType receiverType,
            @Nullable JetType inType,
            @NotNull JetType outType) {
        this(
                original,
                original.getContainingDeclaration(),
                original.getAnnotations(), // TODO : substitute?
                original.getModifiers(),
                original.isVar,
                receiverType,
                original.getName(),
                inType,
                outType);
    }

    public void initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @Nullable PropertyGetterDescriptor getter, @Nullable PropertySetterDescriptor setter) {
        this.typeParemeters.addAll(typeParameters);
        this.getter = getter;
        this.setter = setter;
    }

    @NotNull
    public List<TypeParameterDescriptor> getTypeParemeters() {
        return typeParemeters;
    }

    @Nullable
    public JetType getReceiverType() {
        return receiverType;
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
        JetType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }
        JetType receiverType = getReceiverType();
        return new PropertyDescriptor(
                this,
                receiverType == null ? null : substitutor.substitute(receiverType, Variance.IN_VARIANCE),
                inType,
                outType
        );
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }

    @NotNull
    @Override
    public PropertyDescriptor getOriginal() {
        return original;
    }
}
