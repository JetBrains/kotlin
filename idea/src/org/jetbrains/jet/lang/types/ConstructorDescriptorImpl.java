package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {

    private final boolean isPrimary;

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull List<Attribute> attributes, boolean isPrimary) {
        super(containingDeclaration, attributes, "<init>");
        this.isPrimary = isPrimary;
    }

    public ConstructorDescriptorImpl(@NotNull ConstructorDescriptor original, @NotNull List<Attribute> attributes, boolean isPrimary) {
        super(original, attributes, "<init>");
        this.isPrimary = isPrimary;
    }

    @Override
    @Deprecated
    public FunctionDescriptor initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, @NotNull JetType unsubstitutedReturnType) {
        return super.initialize(typeParameters, unsubstitutedValueParameters, unsubstitutedReturnType);
    }

    public ConstructorDescriptor initialize(@NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters) {
        super.initialize(Collections.<TypeParameterDescriptor>emptyList(), unsubstitutedValueParameters, getContainingDeclaration().getDefaultType());
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
    public List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy() {
        return new ConstructorDescriptorImpl(
                this,
                Collections.<Attribute>emptyList(), // TODO
                isPrimary);
    }
}
