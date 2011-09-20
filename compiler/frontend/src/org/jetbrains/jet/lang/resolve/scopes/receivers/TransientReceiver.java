package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * This represents the receiver of hasNext and next() in for-loops
 * Cannot be an expression receiver because there is no expression for the iterator() call
 *
 * @author abreslav
 */
public class TransientReceiver extends AbstractReceiverDescriptor {
    public TransientReceiver(@NotNull JetType type) {
        super(type);
    }
}
