package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.DataFlowInfo;

import java.util.Collection;

/**
 * @author abreslav
 */
/*package*/ class ResolutionTask<D extends CallableDescriptor> {
    private final Call call;
    private final Collection<ResolvedCall<D>> candidates;
    private final DataFlowInfo dataFlowInfo;
    private DescriptorCheckStrategy checkingStrategy;


    public ResolutionTask(
            @NotNull Collection<ResolvedCall<D>> candidates,
            @NotNull Call call,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        this.candidates = candidates;
        this.call = call;
        this.dataFlowInfo = dataFlowInfo;
    }

    @NotNull
    public DataFlowInfo getDataFlowInfo() {
        return dataFlowInfo;
    }

    @NotNull
    public Collection<ResolvedCall<D>> getCandidates() {
        return candidates;
    }

    @NotNull
    public Call getCall() {
        return call;
    }

    public void setCheckingStrategy(DescriptorCheckStrategy strategy) {
        checkingStrategy = strategy;
    }

    public boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
        if (checkingStrategy != null && !checkingStrategy.performAdvancedChecks(descriptor, trace, tracing)) {
            return false;
        }
        return true;
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }
}
