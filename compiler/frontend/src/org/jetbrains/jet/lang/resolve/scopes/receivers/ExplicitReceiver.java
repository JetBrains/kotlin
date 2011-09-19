package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class ExplicitReceiver implements ReceiverDescriptor {

    private final JetType type;

    public ExplicitReceiver(@NotNull JetType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public JetType getReceiverType() {
        return type;
    }
}
