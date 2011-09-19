package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * Describes a "this" receiver
 *
 * @author abreslav
 */
public abstract class ImplicitReceiverDescriptor implements ReceiverDescriptor {
    private final JetType receiverType;
    private final DeclarationDescriptor declarationDescriptor;

    protected ImplicitReceiverDescriptor(DeclarationDescriptor declarationDescriptor, JetType receiverType) {
        this.receiverType = receiverType;
        this.declarationDescriptor = declarationDescriptor;
    }

    @Override
    @NotNull
    public JetType getReceiverType() {
        return receiverType;
    }

    @NotNull
    public DeclarationDescriptor getDeclarationDescriptor() {
        return declarationDescriptor;
    }
}

