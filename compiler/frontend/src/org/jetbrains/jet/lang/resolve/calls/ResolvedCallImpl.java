package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.calls.ResolutionStatus.UNKNOWN_STATUS;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class ResolvedCallImpl<D extends CallableDescriptor> implements ResolvedCall<D> {

    public static final Function<ResolvedCallImpl<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_CANDIDATE = new Function<ResolvedCallImpl<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCallImpl<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getCandidateDescriptor();
        }
    };

    public static final Function<ResolvedCallImpl<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_RESULT = new Function<ResolvedCallImpl<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCallImpl<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getResultingDescriptor();
        }
    };

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(@NotNull D descriptor) {
        return new ResolvedCallImpl<D>(descriptor);
    }

    @NotNull
    public static <D extends CallableDescriptor> List<ResolvedCallImpl<D>> convertCollection(@NotNull Collection<? extends D> descriptors) {
        List<ResolvedCallImpl<D>> result = Lists.newArrayList();
        for (D descriptor : descriptors) {
            result.add(create(descriptor));
        }
        return result;
    }

    private final D candidateDescriptor;
    private D resultingDescriptor; // Probably substituted
    private ReceiverDescriptor thisObject = NO_RECEIVER; // receiver object of a method
    private ReceiverDescriptor receiverArgument = NO_RECEIVER; // receiver of an extension function

    private final Map<TypeParameterDescriptor, JetType> typeArguments = Maps.newLinkedHashMap();
    private final Map<ValueParameterDescriptor, JetType> autoCasts = Maps.newHashMap();
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = Maps.newHashMap();
    private boolean someArgumentHasNoType = false;
    private TemporaryBindingTrace trace;
    private ResolutionStatus status = UNKNOWN_STATUS;

    private ResolvedCallImpl(@NotNull D candidateDescriptor) {
        this.candidateDescriptor = candidateDescriptor;
    }

    @NotNull
    public ResolutionStatus getStatus() {
        return status;
    }

    public void setStatus(@NotNull ResolutionStatus status) {
        this.status = status;
    }

    @NotNull
    public TemporaryBindingTrace getTrace() {
        return trace;
    }

    public void setTrace(@NotNull TemporaryBindingTrace trace) {
        this.trace = trace;
    }

    @Override
    @NotNull
    public D getCandidateDescriptor() {
        return candidateDescriptor;
    }

    @Override
    @NotNull
    public D getResultingDescriptor() {
        return resultingDescriptor == null ? candidateDescriptor : resultingDescriptor;
    }

    public ResolvedCallImpl<D> setResultingDescriptor(@NotNull D resultingDescriptor) {
        this.resultingDescriptor = resultingDescriptor;
        return this;
    }

    public void recordTypeArgument(@NotNull TypeParameterDescriptor typeParameter, @NotNull JetType typeArgument) {
        assert !typeArguments.containsKey(typeParameter);
        typeArguments.put(typeParameter, typeArgument);
    }

    public void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument) {
        assert !valueArguments.containsKey(valueParameter);
        valueArguments.put(valueParameter, valueArgument);
    }

    public void autoCastValueArgument(@NotNull ValueParameterDescriptor parameter, @NotNull JetType target) {
        assert !autoCasts.containsKey(parameter);
        autoCasts.put(parameter, target);
    }

    @Override
    @NotNull
    public ReceiverDescriptor getReceiverArgument() {
        return receiverArgument;
    }

    public void setReceiverArgument(@NotNull ReceiverDescriptor receiverParameter) {
        this.receiverArgument = receiverParameter;
    }

    @Override
    @NotNull
    public ReceiverDescriptor getThisObject() {
        return thisObject;
    }

    public void setThisObject(@NotNull ReceiverDescriptor thisObject) {
        this.thisObject = thisObject;
    }

    @Override
    @NotNull
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return valueArguments;
    }

    public void argumentHasNoType() {
        this.someArgumentHasNoType = true;
    }

    public boolean isDirty() {
        return someArgumentHasNoType;
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, JetType> getTypeArguments() {
        return typeArguments;
    }
}
