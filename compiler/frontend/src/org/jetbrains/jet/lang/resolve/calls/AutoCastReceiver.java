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
    private final boolean canCast;

    public AutoCastReceiver(@NotNull ReceiverDescriptor original, @NotNull JetType castTo, boolean canCast) {
        super(castTo);
        this.original = original;
        this.canCast = canCast;
    }

    public boolean canCast() {
        return canCast;
    }

    @NotNull
    public ReceiverDescriptor getOriginal() {
        return original;
    }

    @Override
    public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data) {
        return original.accept(visitor, data);
    }

    @Override
    public String toString() {
        return "(" + original + " as " + getType() + ")";
    }
}
