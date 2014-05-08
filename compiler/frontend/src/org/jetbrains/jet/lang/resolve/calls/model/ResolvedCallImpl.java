/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.calls.model;

import com.google.common.collect.Maps;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.INCOMPLETE_TYPE_INFERENCE;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.UNKNOWN_STATUS;

public class ResolvedCallImpl<D extends CallableDescriptor> implements MutableResolvedCall<D> {

    public static final Function<MutableResolvedCall<?>, CallableDescriptor> MAP_TO_CANDIDATE = new Function<MutableResolvedCall<?>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(MutableResolvedCall<?> resolvedCall) {
            return resolvedCall.getCandidateDescriptor();
        }
    };

    public static final Function<MutableResolvedCall<?>, CallableDescriptor> MAP_TO_RESULT = new Function<MutableResolvedCall<?>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(MutableResolvedCall<?> resolvedCall) {
            return resolvedCall.getResultingDescriptor();
        }
    };

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(
            @NotNull ResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new ResolvedCallImpl<D>(candidate, trace, tracing, dataFlowInfoForArguments);
    }

    private final Call call;
    private final D candidateDescriptor;
    private D resultingDescriptor; // Probably substituted
    private final ReceiverValue thisObject; // receiver object of a method
    private final ReceiverValue receiverArgument; // receiver of an extension function
    private final ExplicitReceiverKind explicitReceiverKind;
    private final boolean isSafeCall;

    private final Map<TypeParameterDescriptor, JetType> typeArguments = Maps.newLinkedHashMap();
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = Maps.newLinkedHashMap();
    private final MutableDataFlowInfoForArguments dataFlowInfoForArguments;
    private final Map<ValueArgument, ArgumentMatchImpl> argumentToParameterMap = Maps.newHashMap();

    private DelegatingBindingTrace trace;
    private TracingStrategy tracing;
    private ResolutionStatus status = UNKNOWN_STATUS;
    private ConstraintSystem constraintSystem = null;
    private Boolean hasInferredReturnType = null;
    private boolean completed = false;

    private ResolvedCallImpl(
            @NotNull ResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        this.call = candidate.getCall();
        this.candidateDescriptor = candidate.getDescriptor();
        this.thisObject = candidate.getThisObject();
        this.receiverArgument = candidate.getReceiverArgument();
        this.explicitReceiverKind = candidate.getExplicitReceiverKind();
        this.isSafeCall = candidate.isSafeCall();
        this.trace = trace;
        this.tracing = tracing;
        this.dataFlowInfoForArguments = dataFlowInfoForArguments;
    }

    @Override
    @NotNull
    public ResolutionStatus getStatus() {
        return status;
    }

    @Override
    public void addStatus(@NotNull ResolutionStatus status) {
        this.status = this.status.combine(status);
    }

    @Override
    public void setStatusToSuccess() {
        assert status == INCOMPLETE_TYPE_INFERENCE || status == UNKNOWN_STATUS;
        status = ResolutionStatus.SUCCESS;
    }

    @Override
    @NotNull
    public DelegatingBindingTrace getTrace() {
        assertNotCompleted("Trace");
        return trace;
    }

    @NotNull
    public TracingStrategy getTracing() {
        assertNotCompleted("TracingStrategy");
        return tracing;
    }

    @NotNull
    @Override
    public Call getCall() {
        return call;
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

    @Override
    public void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor) {
        resultingDescriptor = (D) candidateDescriptor.substitute(substitutor);
        assert resultingDescriptor != null : candidateDescriptor;

        for (TypeParameterDescriptor typeParameter : candidateDescriptor.getTypeParameters()) {
            TypeProjection typeArgumentProjection = substitutor.getSubstitution().get(typeParameter.getTypeConstructor());
            if (typeArgumentProjection != null) {
                typeArguments.put(typeParameter, typeArgumentProjection.getType());
            }
        }

        Map<ValueParameterDescriptor, ValueParameterDescriptor> substitutedParametersMap = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameterDescriptor : resultingDescriptor.getValueParameters()) {
            substitutedParametersMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
        }

        Map<ValueParameterDescriptor, ResolvedValueArgument> originalValueArguments = Maps.newLinkedHashMap(valueArguments);
        valueArguments.clear();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : originalValueArguments.entrySet()) {
            ValueParameterDescriptor substitutedVersion = substitutedParametersMap.get(entry.getKey().getOriginal());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }

        Map<ValueArgument, ArgumentMatchImpl> originalArgumentToParameterMap = Maps.newLinkedHashMap(argumentToParameterMap);
        argumentToParameterMap.clear();
        for (Map.Entry<ValueArgument, ArgumentMatchImpl> entry : originalArgumentToParameterMap.entrySet()) {
            ArgumentMatchImpl argumentMatch = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = argumentMatch.getValueParameter();
            ValueParameterDescriptor substitutedVersion = substitutedParametersMap.get(valueParameterDescriptor.getOriginal());
            assert substitutedVersion != null : valueParameterDescriptor;
            argumentToParameterMap.put(entry.getKey(), argumentMatch.replaceValueParameter(substitutedVersion));
        }
    }

    @Override
    public void setConstraintSystem(@NotNull ConstraintSystem constraintSystem) {
        this.constraintSystem = constraintSystem;
    }

    @Nullable
    @Override
    public ConstraintSystem getConstraintSystem() {
        assertNotCompleted("ConstraintSystem");
        return constraintSystem;
    }

    @Override
    public void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument) {
        assert !valueArguments.containsKey(valueParameter) : valueParameter + " -> " + valueArgument;
        valueArguments.put(valueParameter, valueArgument);
        for (ValueArgument argument : valueArgument.getArguments()) {
            argumentToParameterMap.put(argument, new ArgumentMatchImpl(valueParameter));
        }
    }

    @Override
    @NotNull
    public ReceiverValue getReceiverArgument() {
        return receiverArgument;
    }

    @Override
    @NotNull
    public ReceiverValue getThisObject() {
        return thisObject;
    }

    @Override
    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @Override
    @NotNull
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return valueArguments;
    }

    @Nullable
    @Override
    public List<ResolvedValueArgument> getValueArgumentsByIndex() {
        List<ResolvedValueArgument> arguments = new ArrayList<ResolvedValueArgument>(candidateDescriptor.getValueParameters().size());
        for (int i = 0; i < candidateDescriptor.getValueParameters().size(); ++i) {
            arguments.add(null);
        }
        
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument value = entry.getValue();
            ResolvedValueArgument oldValue = arguments.set(parameterDescriptor.getIndex(), value);
            if (oldValue != null) {
                return null;
            }
        }

        for (int i = 0; i < arguments.size(); i++) {
            Object o = arguments.get(i);
            if (o == null) {
                return null;
            }
        }
        
        return arguments;
    }

    @Override
    public void recordArgumentMatchStatus(@NotNull ValueArgument valueArgument, @NotNull ArgumentMatchStatus matchStatus) {
        ArgumentMatchImpl argumentMatch = argumentToParameterMap.get(valueArgument);
        argumentMatch.recordMatchStatus(matchStatus);
    }

    @NotNull
    @Override
    public ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument) {
        ArgumentMatch argumentMatch = argumentToParameterMap.get(valueArgument);
        if (argumentMatch == null) {
            return ArgumentUnmapped.instance$;
        }
        return argumentMatch;
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, JetType> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public boolean isSafeCall() {
        return isSafeCall;
    }

    @NotNull
    @Override
    public MutableDataFlowInfoForArguments getDataFlowInfoForArguments() {
        return dataFlowInfoForArguments;
    }

    @Override
    public boolean hasInferredReturnType() {
        if (!completed) {
            hasInferredReturnType = constraintSystem == null || CallResolverUtil.hasInferredReturnType(candidateDescriptor, constraintSystem);
        }
        assert hasInferredReturnType != null : "The property 'hasInferredReturnType' was not set when the call was completed.";
        return hasInferredReturnType;
    }

    @Override
    public void markCallAsCompleted() {
        if (!completed) {
            hasInferredReturnType();
        }
        trace = null;
        constraintSystem = null;
        tracing = null;
        completed = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    private void assertNotCompleted(String elementName) {
        assert !completed: elementName + " is erased after resolution completion.";
    }
}
