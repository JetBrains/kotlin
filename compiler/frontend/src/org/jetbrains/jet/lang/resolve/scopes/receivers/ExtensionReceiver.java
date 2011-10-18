package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ExtensionReceiver extends AbstractReceiverDescriptor implements ThisReceiverDescriptor {

    private final CallableDescriptor descriptor;

    public ExtensionReceiver(@NotNull CallableDescriptor callableDescriptor, @NotNull JetType receiverType) {
        super(receiverType);
        this.descriptor = callableDescriptor;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getDeclarationDescriptor() {
        return descriptor;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitExtensionReceiver(this, data);
    }
}
