/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.model;

import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilKt;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.*;

import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.INCOMPLETE_TYPE_INFERENCE;
import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.UNKNOWN_STATUS;

public class ResolvedCallImpl<D extends CallableDescriptor> implements MutableResolvedCall<D> {
    private static final Logger LOG = Logger.getInstance(ResolvedCallImpl.class);

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(
            @NotNull ResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new ResolvedCallImpl<>(candidate, trace, tracing, dataFlowInfoForArguments);
    }

    private final Call call;
    private final D candidateDescriptor;
    private D resultingDescriptor; // Probably substituted
    private final ReceiverValue dispatchReceiver; // receiver object of a method
    private ReceiverValue extensionReceiver; // receiver of an extension function
    private final ExplicitReceiverKind explicitReceiverKind;
    private final TypeSubstitutor knownTypeParametersSubstitutor;

    @NotNull
    private final Map<TypeParameterDescriptor, KotlinType> typeArguments;
    @NotNull
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments;
    private final MutableDataFlowInfoForArguments dataFlowInfoForArguments;
    @NotNull
    private final Map<ValueArgument, ArgumentMatchImpl> argumentToParameterMap;

    private DelegatingBindingTrace trace;
    private TracingStrategy tracing;
    private ResolutionStatus status = UNKNOWN_STATUS;
    private ConstraintSystem constraintSystem = null;
    private Boolean hasInferredReturnType = null;
    private boolean completed = false;
    private KotlinType smartCastDispatchReceiverType = null;

    private ResolvedCallImpl(
            @NotNull ResolutionCandidate<D> candidate,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        this.call = candidate.getCall();
        this.candidateDescriptor = candidate.getDescriptor();
        this.dispatchReceiver = candidate.getDispatchReceiver();
        this.extensionReceiver = null; // ResolutionCandidate can have only dispatch receiver
        this.explicitReceiverKind = candidate.getExplicitReceiverKind();
        this.knownTypeParametersSubstitutor = candidate.getKnownTypeParametersResultingSubstitutor();
        this.trace = trace;
        this.tracing = tracing;
        this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        this.typeArguments = createTypeArgumentsMap(candidateDescriptor);
        this.valueArguments = createValueArgumentsMap(candidateDescriptor);
        this.argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor);
    }

    public ResolvedCallImpl(
            @NotNull Call call,
            @NotNull D candidateDescriptor,
            @Nullable ReceiverValue dispatchReceiver,
            @Nullable ReceiverValue extensionReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersSubstitutor,
            @NotNull DelegatingBindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        this.call = call;
        this.candidateDescriptor = candidateDescriptor;
        this.dispatchReceiver = dispatchReceiver;
        this.extensionReceiver = extensionReceiver;
        this.explicitReceiverKind = explicitReceiverKind;
        this.knownTypeParametersSubstitutor = knownTypeParametersSubstitutor;
        this.trace = trace;
        this.tracing = tracing;
        this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        this.typeArguments = createTypeArgumentsMap(candidateDescriptor);
        this.valueArguments = createValueArgumentsMap(candidateDescriptor);
        this.argumentToParameterMap = createArgumentsToParameterMap(candidateDescriptor);
    }

    @NotNull
    private static Map<ValueParameterDescriptor, ResolvedValueArgument> createValueArgumentsMap(CallableDescriptor descriptor) {
        return descriptor.getValueParameters().isEmpty() ? Collections.emptyMap() : Maps.newLinkedHashMap();
    }

    @NotNull
    private static Map<ValueArgument, ArgumentMatchImpl> createArgumentsToParameterMap(CallableDescriptor descriptor) {
        return descriptor.getValueParameters().isEmpty() ? Collections.emptyMap() : Maps.newHashMap();
    }

    @NotNull
    private static Map<TypeParameterDescriptor, KotlinType> createTypeArgumentsMap(CallableDescriptor descriptor) {
        return descriptor.getTypeParameters().isEmpty() ? Collections.emptyMap() : Maps.newLinkedHashMap();
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

    @Override
    @NotNull
    public TracingStrategy getTracingStrategy() {
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
            TypeProjection typeArgumentProjection = substitutor.getSubstitution().get(typeParameter.getDefaultType());
            if (typeArgumentProjection != null) {
                typeArguments.put(typeParameter, typeArgumentProjection.getType());
            }
        }

        if (candidateDescriptor.getValueParameters().isEmpty()) return;

        List<ValueParameterDescriptor> substitutedParameters = resultingDescriptor.getValueParameters();

        Collection<Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>> valueArgumentsBeforeSubstitution =
                new SmartList<>(valueArguments.entrySet());

        valueArguments.clear();

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArgumentsBeforeSubstitution) {
            ValueParameterDescriptor substitutedVersion = substitutedParameters.get(entry.getKey().getIndex());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }

        Collection<Map.Entry<ValueArgument, ArgumentMatchImpl>> unsubstitutedArgumentMappings =
                new SmartList<>(argumentToParameterMap.entrySet());

        argumentToParameterMap.clear();
        for (Map.Entry<ValueArgument, ArgumentMatchImpl> entry : unsubstitutedArgumentMappings) {
            ArgumentMatchImpl argumentMatch = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = argumentMatch.getValueParameter();
            ValueParameterDescriptor substitutedVersion = substitutedParameters.get(valueParameterDescriptor.getIndex());
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
    @Nullable
    public ReceiverValue getExtensionReceiver() {
        return extensionReceiver;
    }

    @Override
    @Nullable
    public ReceiverValue getDispatchReceiver() {
        return dispatchReceiver;
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
        List<ResolvedValueArgument> arguments = new ArrayList<>(candidateDescriptor.getValueParameters().size());
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
            if (ArgumentMappingKt.isReallySuccess(this)) {
                LOG.error("ArgumentUnmapped for " + valueArgument + " in successfully resolved call: " + call.getCallElement().getText());
            }
            return ArgumentUnmapped.INSTANCE;
        }
        return argumentMatch;
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, KotlinType> getTypeArguments() {
        return typeArguments;
    }

    @NotNull
    @Override
    public MutableDataFlowInfoForArguments getDataFlowInfoForArguments() {
        return dataFlowInfoForArguments;
    }

    @Override
    public boolean hasInferredReturnType() {
        if (!completed) {
            hasInferredReturnType = constraintSystem == null ||
                                    CallResolverUtilKt.hasInferredReturnType(candidateDescriptor, constraintSystem);
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

    @Override
    @Nullable
    public TypeSubstitutor getKnownTypeParametersSubstitutor() {
        return knownTypeParametersSubstitutor;
    }

    @Override
    public void setSmartCastDispatchReceiverType(@NotNull KotlinType smartCastDispatchReceiverType) {
        this.smartCastDispatchReceiverType = smartCastDispatchReceiverType;
    }

    @Override
    @Nullable
    public KotlinType getSmartCastDispatchReceiverType() {
        return smartCastDispatchReceiverType;
    }

    @Override
    public void updateExtensionReceiverWithSmartCastIfNeeded(@NotNull KotlinType smartCastExtensionReceiverType) {
        if (extensionReceiver instanceof ImplicitClassReceiver) {
            extensionReceiver = new CastImplicitClassReceiver(
                    ((ImplicitClassReceiver) extensionReceiver).getClassDescriptor(),
                    smartCastExtensionReceiverType
            );
        }
    }
}
