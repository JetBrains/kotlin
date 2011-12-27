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
public class VariableDescriptorBoundToReceiver implements VariableDescriptor {
    private VariableDescriptor variableDescriptor;
    private DeclarationDescriptor receiver;

    public VariableDescriptorBoundToReceiver(VariableDescriptor variableDescriptor, DeclarationDescriptor receiver) {
        this.variableDescriptor = variableDescriptor;
        this.receiver = receiver;
    }

    public VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }

    public DeclarationDescriptor getReceiver() {
        return receiver;
    }

    @Override
    public JetType getOutType() {
        return variableDescriptor.getOutType();
    }

    @Override
    public JetType getInType() {
        return variableDescriptor.getInType();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return variableDescriptor.getContainingDeclaration();
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
        return variableDescriptor.getTypeParameters();
    }

    @Override
    public JetType getReturnType() {
        return variableDescriptor.getReturnType();
    }

    @NotNull
    @Override
    public CallableDescriptor getOriginal() {
        return variableDescriptor.getOriginal();
    }

    @Override
    public VariableDescriptor substitute(TypeSubstitutor substitutor) {
        return variableDescriptor.substitute(substitutor);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return variableDescriptor.accept(visitor, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        variableDescriptor.acceptVoid(visitor);
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return variableDescriptor.getValueParameters();
    }

    @NotNull
    @Override
    public Set<? extends CallableDescriptor> getOverriddenDescriptors() {
        return variableDescriptor.getOverriddenDescriptors();
    }

    @Override
    public boolean isVar() {
        return variableDescriptor.isVar();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return variableDescriptor.getAnnotations();
    }

    @NotNull
    @Override
    public String getName() {
        return variableDescriptor.getName();
    }
}
