package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class AbstractReceiverDescriptor implements ReceiverDescriptor {
    protected final JetType receiverType;

    public AbstractReceiverDescriptor(@NotNull JetType receiverType) {
        this.receiverType = receiverType;
    }

    @Override
    @NotNull
    public JetType getType() {
        return receiverType;
    }

    @Override
    public boolean exists() {
        return true;
    }
}
