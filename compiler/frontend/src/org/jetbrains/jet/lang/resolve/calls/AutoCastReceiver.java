package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.receivers.AbstractReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptorVisitor;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class AutoCastReceiver extends AbstractReceiverDescriptor {
    private final ReceiverDescriptor original;

    public AutoCastReceiver(@NotNull ReceiverDescriptor original, @NotNull JetType castTo) {
        super(castTo);
        this.original = original;
    }

    @NotNull
    public ReceiverDescriptor getOriginal() {
        return original;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        throw new UnsupportedOperationException();
    }
}
