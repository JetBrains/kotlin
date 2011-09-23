package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.DescriptorSubstitutor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class FunctionDescriptorImpl extends DeclarationDescriptorImpl implements FunctionDescriptor {

    private List<TypeParameterDescriptor> typeParameters;
    private List<ValueParameterDescriptor> unsubstitutedValueParameters;
    private JetType unsubstitutedReturnType;
    private ReceiverDescriptor receiver;

    private Modality modality;
    private Visibility visibility;
    private final Set<FunctionDescriptor> overriddenFunctions = Sets.newLinkedHashSet();
    private final FunctionDescriptor original;

    public FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
        this.original = this;
    }

    public FunctionDescriptorImpl(
            @NotNull FunctionDescriptor original,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name) {
        super(original.getContainingDeclaration(), annotations, name);
        this.original = original;
    }

    public FunctionDescriptorImpl initialize(
            @Nullable JetType receiverType,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility) {
        this.typeParameters = typeParameters;
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
        this.modality = modality;
        this.visibility = visibility;
        this.receiver = receiverType == null ? NO_RECEIVER : new ExtensionReceiver(this, receiverType);
        return this;
    }

    public void setReturnType(@NotNull JetType unsubstitutedReturnType) {
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getReceiver() {
        return receiver;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return overriddenFunctions;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    public void addOverriddenFunction(@NotNull FunctionDescriptor overriddenFunction) {
        overriddenFunctions.add(overriddenFunction);
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return unsubstitutedValueParameters;
    }

    @Override
    @NotNull
    public JetType getReturnType() {
        return unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @Override
    public final FunctionDescriptor substitute(TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy();

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType substitutedReceiverType = null;
        if (receiver != NO_RECEIVER) {
            substitutedReceiverType = substitutor.substitute(getReceiver().getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverType == null) {
                return null;
            }
        }

        List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorUtil.getSubstitutedValueParameters(substitutedDescriptor, this, substitutor);
        if (substitutedValueParameters == null) {
            return null;
        }

        JetType substitutedReturnType = FunctionDescriptorUtil.getSubstitutedReturnType(this, substitutor);
        if (substitutedReturnType == null) {
            return null;
        }

        substitutedDescriptor.initialize(
                substitutedReceiverType,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                modality,
                visibility
        );
        for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
            substitutedDescriptor.addOverriddenFunction(overriddenFunction);
        }
        return substitutedDescriptor;
    }

    protected FunctionDescriptorImpl createSubstitutedCopy() {
        return new FunctionDescriptorImpl(
                this,
                // TODO : safeSubstitute
                getAnnotations(),
                getName());
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) return false;
//        if (obj.getClass() != FunctionDescriptorImpl.class) return false;
//        FunctionDescriptorImpl other = (FunctionDescriptorImpl) obj;
//        if (!eq(this.getName(), other.getName())) return false;
//        if (!eq(this.getContainingDeclaration(), other.getContainingDeclaration())) return false;
//
//    }
//
//    private static boolean eq(Object a, Object b) {
//        if (a == null) return b == null;
//        if (b == null) return false;
//        return a.equals(b);
//    }
//
//    @Override
//    public int hashCode() {
//        return super.hashCode(); // TODO
//    }
}
