package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.DataFlowInfo;

import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public interface AutoCastService {
    AutoCastService NO_AUTO_CASTS = new AutoCastService() {
        @Override
        public DataFlowInfo getDataFlowInfo() {
            return DataFlowInfo.getEmpty();
        }

        @Override
        public boolean isNotNull(@NotNull ReceiverDescriptor receiver) {
            return !receiver.getType().isNullable();
        }

        @Override
        public List<ReceiverDescriptor> getVariantsForReceiver(ReceiverDescriptor receiverDescriptor) {
            return Collections.singletonList(receiverDescriptor);
        }
    };

    List<ReceiverDescriptor> getVariantsForReceiver(ReceiverDescriptor receiverDescriptor);
    DataFlowInfo getDataFlowInfo();

    boolean isNotNull(@NotNull ReceiverDescriptor receiver);
}
