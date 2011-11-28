package org.jetbrains.jet.lang.resolve.calls.autocasts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public interface AutoCastService {
    AutoCastService NO_AUTO_CASTS = new AutoCastService() {
        @NotNull
        @Override
        public DataFlowInfo getDataFlowInfo() {
            return DataFlowInfo.EMPTY;
        }

        @Override
        public boolean isNotNull(@NotNull ReceiverDescriptor receiver) {
            return !receiver.getType().isNullable();
        }

        @NotNull
        @Override
        public List<ReceiverDescriptor> getVariantsForReceiver(@NotNull ReceiverDescriptor receiverDescriptor) {
            return Collections.singletonList(receiverDescriptor);
        }
    };

    @NotNull
    List<ReceiverDescriptor> getVariantsForReceiver(@NotNull ReceiverDescriptor receiverDescriptor);

    @NotNull
    DataFlowInfo getDataFlowInfo();

    boolean isNotNull(@NotNull ReceiverDescriptor receiver);
}
