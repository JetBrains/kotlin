package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements MemberDescriptor {

    private final Modality modality;
    private final boolean isVar;
    private final ReceiverDescriptor receiver;
    private final List<TypeParameterDescriptor> typeParemeters = Lists.newArrayListWithCapacity(0);
    private final PropertyDescriptor original;
    private PropertyGetterDescriptor getter;
    private PropertySetterDescriptor setter;

    private PropertyDescriptor(
            @Nullable PropertyDescriptor original,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            boolean isVar,
            @Nullable JetType receiverType,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType) {
        super(containingDeclaration, annotations, name, inType, outType);
        assert !isVar || inType != null;
//        assert outType != null;
        this.isVar = isVar;
        this.modality = modality;
        this.receiver = receiverType == null ? ReceiverDescriptor.NO_RECEIVER : new ExtensionReceiver(this, receiverType);
        this.original = original == null ? this : original.getOriginal();
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            boolean isVar,
            @Nullable JetType receiverType,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType) {
        this(null, containingDeclaration, annotations, modality, isVar, receiverType, name, inType, outType);
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
                original.getModality(),
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
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParemeters;
    }

    @NotNull
    public ReceiverDescriptor getReceiver() {
        return receiver;
    }

    @NotNull
    @Override
    public JetType getReturnType() {
        return getOutType();
    }


    public boolean isVar() {
        return isVar;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
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
    public PropertyDescriptor substitute(TypeSubstitutor substitutor) {
        JetType originalInType = getInType();
        JetType inType = originalInType == null ? null : substitutor.substitute(originalInType, Variance.IN_VARIANCE);
        JetType originalOutType = getOutType();
        JetType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (inType == null && outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }
        return new PropertyDescriptor(
                this,
                receiver.exists() ? substitutor.substitute(receiver.getType(), Variance.IN_VARIANCE) : null,
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
