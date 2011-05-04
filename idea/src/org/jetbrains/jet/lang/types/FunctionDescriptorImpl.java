package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class FunctionDescriptorImpl extends DeclarationDescriptorImpl implements MutableFunctionDescriptor {

    private List<TypeParameterDescriptor> typeParameters;

    private List<ValueParameterDescriptor> unsubstitutedValueParameters;

    private JetType unsubstitutedReturnType;
    private final FunctionDescriptor original;

    public FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
        this.original = this;
    }

    public FunctionDescriptorImpl(
            @NotNull FunctionDescriptor original,
            @NotNull List<Annotation> annotations,
            @NotNull String name) {
        super(original.getContainingDeclaration(), annotations, name);
        this.original = original;
    }

    public FunctionDescriptor initialize(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType) {
        this.typeParameters = typeParameters;
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
        return this;
    }

    @Override
    public void setUnsubstitutedReturnType(@NotNull JetType unsubstitutedReturnType) {
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @Override
    public boolean isReturnTypeSet() {
        return unsubstitutedReturnType != null;
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        return unsubstitutedValueParameters;
    }

    @Override
    @NotNull
    public JetType getUnsubstitutedReturnType() {
        return unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return original;
    }

    @Override
    public final FunctionDescriptor substitute(TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) {
            return this;
        }
        FunctionDescriptorImpl substitutedDescriptor;
        substitutedDescriptor = createSubstitutedCopy();

        List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorUtil.getSubstitutedValueParameters(substitutedDescriptor, this, substitutor);
        if (substitutedValueParameters == null) {
            return null;
        }

        JetType substitutedReturnType = FunctionDescriptorUtil.getSubstitutedReturnType(this, substitutor);
        if (substitutedReturnType == null) {
            return null;
        }

        substitutedDescriptor.initialize(
                Collections.<TypeParameterDescriptor>emptyList(), // TODO : questionable
                substitutedValueParameters,
                substitutedReturnType
        );
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

}
