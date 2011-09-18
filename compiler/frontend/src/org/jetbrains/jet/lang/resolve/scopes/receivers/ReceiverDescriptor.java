package org.jetbrains.jet.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface ReceiverDescriptor {

    ReceiverDescriptor NO_RECEIVER = new ReceiverDescriptor() {
        @NotNull
        @Override
        public JetType getReceiverType() {
            return JetStandardClasses.getNothingType();
        }

        @Override
        public String toString() {
            return "NO_RECEIVER";
        }
    };

    @NotNull
    JetType getReceiverType();
}
