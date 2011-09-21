package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * Describes a "this" receiver
 *
 * @author abreslav
 */
public abstract class ImplicitReceiverDescriptor extends AbstractReceiverDescriptor {
    private final DeclarationDescriptor declarationDescriptor;

    protected ImplicitReceiverDescriptor(@NotNull DeclarationDescriptor declarationDescriptor, @NotNull JetType receiverType) {
        super(receiverType);
        this.declarationDescriptor = declarationDescriptor;
    }

    @NotNull
    public DeclarationDescriptor getDeclarationDescriptor() {
        return declarationDescriptor;
    }
}

