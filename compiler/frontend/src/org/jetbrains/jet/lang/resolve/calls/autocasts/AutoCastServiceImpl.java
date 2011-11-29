package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.List;

/**
* @author abreslav
*/
public class AutoCastServiceImpl implements AutoCastService {
    private final DataFlowInfo dataFlowInfo;
    private final BindingContext bindingContext;

    public AutoCastServiceImpl(DataFlowInfo dataFlowInfo, BindingContext bindingContext) {
        this.dataFlowInfo = dataFlowInfo;
        this.bindingContext = bindingContext;
    }

    @NotNull
    @Override
    public List<ReceiverDescriptor> getVariantsForReceiver(@NotNull ReceiverDescriptor receiverDescriptor) {
        List<ReceiverDescriptor> variants = Lists.newArrayList(AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiverDescriptor));
        variants.add(receiverDescriptor);
        return variants;
    }

    @NotNull
    @Override
    public DataFlowInfo getDataFlowInfo() {
        return dataFlowInfo;
    }

    @Override
    public boolean isNotNull(@NotNull ReceiverDescriptor receiver) {
        if (!receiver.getType().isNullable()) return true;

        List<ReceiverDescriptor> autoCastVariants = AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiver);
        for (ReceiverDescriptor autoCastVariant : autoCastVariants) {
            if (!autoCastVariant.getType().isNullable()) return true;
        }
        return false;
    }
}
