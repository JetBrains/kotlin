package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetTypeProjection;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
/*package*/ class ResolutionTask<Descriptor extends CallableDescriptor> {
    private final Collection<Descriptor> candidates;
    private final ReceiverDescriptor receiver;
    private final List<JetTypeProjection> typeArguments;
    private final List<? extends ValueArgument> valueArguments;
    private final List<JetExpression> functionLiteralArguments;
    private DescriptorCheckStrategy checkingStrategy;

    public ResolutionTask(
            @NotNull Collection<Descriptor> candidates,
            @NotNull ReceiverDescriptor receiver,
            @NotNull List<JetTypeProjection> typeArguments,
            @NotNull List<? extends ValueArgument> valueArguments,
            @NotNull List<JetExpression> functionLiteralArguments) {
        this.candidates = candidates;
        this.receiver = receiver;
        this.typeArguments = typeArguments;
        this.valueArguments = valueArguments;
        this.functionLiteralArguments = functionLiteralArguments;
    }

    public ResolutionTask(
            @NotNull Collection<Descriptor> candidates,
            @NotNull ReceiverDescriptor receiver,
            @NotNull Call call
    ) {
        this(candidates, receiver, call.getTypeArguments(), call.getValueArguments(), call.getFunctionLiteralArguments());
    }

    @NotNull
    public Collection<Descriptor> getCandidates() {
        return candidates;
    }

    @NotNull
    public ReceiverDescriptor getReceiver() {
        return receiver;
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        return typeArguments;
    }

    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        return valueArguments;
    }

    @NotNull
    public List<JetExpression> getFunctionLiteralArguments() {
        return functionLiteralArguments;
    }
    
    public void setCheckingStrategy(DescriptorCheckStrategy strategy) {
        checkingStrategy = strategy;
    }

    public boolean performAdvancedChecks(Descriptor descriptor, BindingTrace trace, TracingStrategy tracing) {
        if (checkingStrategy != null && !checkingStrategy.performAdvancedChecks(descriptor, trace, tracing)) {
            return false;
        }
        return true;
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }
}
