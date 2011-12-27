package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;
import java.util.Set;

/**
 * @author svtk
 */
public class FunctionDescriptorWithImplicitReceiver implements FunctionDescriptor {

    private FunctionDescriptor functionDescriptor;
    private DeclarationDescriptor implicitReceiver;

    public FunctionDescriptorWithImplicitReceiver(@NotNull FunctionDescriptor functionDescriptor, @NotNull DeclarationDescriptor implicitReceiver) {
        this.functionDescriptor = functionDescriptor;
        this.implicitReceiver = implicitReceiver;
    }

    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    public DeclarationDescriptor getImplicitReceiver() {
        return implicitReceiver;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return functionDescriptor.getContainingDeclaration();
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return functionDescriptor.getOriginal();
    }

    @Override
    public FunctionDescriptor substitute(TypeSubstitutor substitutor) {
        return functionDescriptor.substitute(substitutor);
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return functionDescriptor.getOverriddenDescriptors();
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        return functionDescriptor.copy(newOwner, makeNonAbstract);
    }

    @NotNull
    @Override
    public ReceiverDescriptor getReceiverParameter() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return functionDescriptor.getTypeParameters();
    }

    @Override
    public JetType getReturnType() {
        return functionDescriptor.getReturnType();
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return functionDescriptor.getValueParameters();
    }

    @NotNull
    @Override
    public Modality getModality() {
        return functionDescriptor.getModality();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return functionDescriptor.getVisibility();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return functionDescriptor.accept(visitor, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        functionDescriptor.acceptVoid(visitor);
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return functionDescriptor.getAnnotations();
    }

    @NotNull
    @Override
    public String getName() {
        return functionDescriptor.getName();
    }
}
