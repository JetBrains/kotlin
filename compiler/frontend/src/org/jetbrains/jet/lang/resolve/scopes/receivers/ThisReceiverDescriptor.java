package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * Describes a "this" receiver
 *
 * @author abreslav
 */
public interface ThisReceiverDescriptor extends ReceiverDescriptor {
    @NotNull
    DeclarationDescriptor getDeclarationDescriptor();
}

