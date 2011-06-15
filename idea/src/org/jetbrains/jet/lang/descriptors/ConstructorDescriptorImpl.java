package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class ConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {

    private final boolean isPrimary;

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull List<Annotation> annotations, boolean isPrimary) {
        super(containingDeclaration, annotations, "<init>");
        this.isPrimary = isPrimary;
    }

    public ConstructorDescriptorImpl(@NotNull ConstructorDescriptor original, @NotNull List<Annotation> annotations, boolean isPrimary) {
        super(original, annotations, "<init>");
        this.isPrimary = isPrimary;
    }

    @Override
    @Deprecated
    public FunctionDescriptorImpl initialize(@Nullable JetType receiverType, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, @Nullable JetType unsubstitutedReturnType) {
        assert receiverType == null;
        return super.initialize(null, typeParameters, unsubstitutedValueParameters, unsubstitutedReturnType);
    }

    public ConstructorDescriptorImpl initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters) {
        super.initialize(null, typeParameters, unsubstitutedValueParameters, null);
        return this;
    }

    @NotNull
    @Override
    public ClassDescriptor getContainingDeclaration() {
        return (ClassDescriptor) super.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ConstructorDescriptor getOriginal() {
        return (ConstructorDescriptor) super.getOriginal();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorDescriptor(this, data);
    }

    @Override
    public boolean isPrimary() {
        return isPrimary;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenFunctions() {
        return null;
    }

    @Override
    public void addOverriddenFunction(@NotNull FunctionDescriptor overriddenFunction) {
        throw new UnsupportedOperationException("Constructors cannot override anything");
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy() {
        return new ConstructorDescriptorImpl(
                this,
                Collections.<Annotation>emptyList(), // TODO
                isPrimary);
    }
}
