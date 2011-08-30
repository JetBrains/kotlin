package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetTypeProjection;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
/*package*/ class ResolutionTask<Descriptor extends CallableDescriptor> {
    private final Collection<Descriptor> candidates;
    private final JetType receiverType;
    private final List<JetTypeProjection> typeArguments;
    private final List<? extends ValueArgument> valueArguments;
    private final List<JetExpression> functionLiteralArguments;

    public ResolutionTask(
            @NotNull Collection<Descriptor> candidates,
            @Nullable JetType receiverType,
            @NotNull List<JetTypeProjection> typeArguments,
            @NotNull List<? extends ValueArgument> valueArguments,
            @NotNull List<JetExpression> functionLiteralArguments) {
        this.candidates = candidates;
        this.receiverType = receiverType;
        this.typeArguments = typeArguments;
        this.valueArguments = valueArguments;
        this.functionLiteralArguments = functionLiteralArguments;
    }

    public ResolutionTask(
            @NotNull Collection<Descriptor> candidates,
            @Nullable JetType receiverType,
            @NotNull Call call
    ) {
        this(candidates, receiverType, call.getTypeArguments(), call.getValueArguments(), call.getFunctionLiteralArguments());
    }

    @NotNull
    public Collection<Descriptor> getCandidates() {
        return candidates;
    }

    @Nullable
    public JetType getReceiverType() {
        return receiverType;
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

}
