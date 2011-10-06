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

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class ResolvedCall<D extends CallableDescriptor> {

    public static final Function<ResolvedCall<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_CANDIDATE = new Function<ResolvedCall<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCall<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getCandidateDescriptor();
        }
    };

    public static final Function<ResolvedCall<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_RESULT = new Function<ResolvedCall<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCall<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getResultingDescriptor();
        }
    };

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCall<D> create(@NotNull D descriptor) {
        return new ResolvedCall<D>(descriptor);
    }

    @NotNull
    public static <D extends CallableDescriptor> List<ResolvedCall<D>> convertCollection(@NotNull Collection<D> descriptors) {
        List<ResolvedCall<D>> result = Lists.newArrayList();
        for (D descriptor : descriptors) {
            result.add(create(descriptor));
        }
        return result;
    }

    private final D candidateDescriptor;
    private D resultingDescriptor; // Probably substituted
    private ReceiverDescriptor thisObject = NO_RECEIVER; // receiver object of a method
    private ReceiverDescriptor receiverParameter = NO_RECEIVER; // receiver of an extension function
    private final Map<TypeParameterDescriptor, JetType> typeArguments = Maps.newHashMap();
    private final Map<ValueParameterDescriptor, JetType> autoCasts = Maps.newHashMap();
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = Maps.newHashMap();
    private TemporaryBindingTrace trace;

    private ResolvedCall(@NotNull D candidateDescriptor) {
        this.candidateDescriptor = candidateDescriptor;
    }

    @NotNull
    public TemporaryBindingTrace getTrace() {
        return trace;
    }

    public void setTrace(@NotNull TemporaryBindingTrace trace) {
        this.trace = trace;
    }

    @NotNull
    public D getCandidateDescriptor() {
        return candidateDescriptor;
    }

    @NotNull
    public D getResultingDescriptor() {
        return resultingDescriptor == null ? candidateDescriptor : resultingDescriptor;
    }

    public ResolvedCall<D> setResultingDescriptor(@NotNull D resultingDescriptor) {
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

    @NotNull
    public ReceiverDescriptor getReceiverParameter() {
        return receiverParameter;
    }

    public void setReceiverParameter(@NotNull ReceiverDescriptor receiverParameter) {
        this.receiverParameter = receiverParameter;
    }

    @NotNull
    public ReceiverDescriptor getThisObject() {
        return thisObject;
    }

    public void setThisObject(@NotNull ReceiverDescriptor thisObject) {
        this.thisObject = thisObject;
    }

}
