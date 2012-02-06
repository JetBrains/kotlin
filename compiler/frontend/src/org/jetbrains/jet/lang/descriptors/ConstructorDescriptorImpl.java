package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class ConstructorDescriptorImpl extends FunctionDescriptorImpl implements ConstructorDescriptor {

    private final boolean isPrimary;

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean isPrimary) {
        super(containingDeclaration, annotations, "<init>");
        this.isPrimary = isPrimary;
    }

    public ConstructorDescriptorImpl(@NotNull ClassDescriptor containingDeclaration, @NotNull ConstructorDescriptor original, @NotNull List<AnnotationDescriptor> annotations, boolean isPrimary) {
        super(containingDeclaration, original, annotations, "<init>");
        this.isPrimary = isPrimary;
    }

    @Override
    @Deprecated
    public ConstructorDescriptorImpl initialize(@Nullable JetType receiverType, @NotNull ReceiverDescriptor expectedThisObject, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, @Nullable JetType unsubstitutedReturnType, Modality modality, @NotNull Visibility visibility) {
        assert receiverType == null;
        return (ConstructorDescriptorImpl) super.initialize(null, expectedThisObject, typeParameters, unsubstitutedValueParameters, unsubstitutedReturnType, modality, visibility);
    }

    public ConstructorDescriptorImpl initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters, Modality modality, Visibility visibility) {
        super.initialize(null, getExpectedThisObject(getContainingDeclaration()), typeParameters, unsubstitutedValueParameters, null, modality, visibility);
        return this;
    }

    @NotNull
    private static ReceiverDescriptor getExpectedThisObject(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) {
            ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) descriptor;
            ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();
            return getExpectedThisObject(classDescriptor);
        }
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        return DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration);
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
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();
    }

    @Override
    public void addOverriddenFunction(@NotNull FunctionDescriptor overriddenFunction) {
        throw new UnsupportedOperationException("Constructors cannot override anything");
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner) {
        return new ConstructorDescriptorImpl(
                (ClassDescriptor) newOwner,
                this,
                Collections.<AnnotationDescriptor>emptyList(), // TODO
                isPrimary);
    }

    @NotNull
    @Override
    public ConstructorDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        throw new UnsupportedOperationException("Constructors should not be copied for overriding");
    }
}
