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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

public class VariableAsFunctionResolvedCall implements ResolvedCallWithTrace<FunctionDescriptor> {
    private final ResolvedCallWithTrace<FunctionDescriptor> functionCall;
    private final ResolvedCallWithTrace<VariableDescriptor> variableCall;

    public VariableAsFunctionResolvedCall(@NotNull ResolvedCallWithTrace<FunctionDescriptor> functionCall,
            @NotNull ResolvedCallWithTrace<VariableDescriptor> variableCall) {
        this.functionCall = functionCall;
        this.variableCall = variableCall;
    }

    public ResolvedCallWithTrace<FunctionDescriptor> getFunctionCall() {
        return functionCall;
    }

    public ResolvedCallWithTrace<VariableDescriptor> getVariableCall() {
        return variableCall;
    }

    @NotNull
    @Override
    public FunctionDescriptor getCandidateDescriptor() {
        return functionCall.getResultingDescriptor();
    }

    @NotNull
    @Override
    public FunctionDescriptor getResultingDescriptor() {
        return functionCall.getResultingDescriptor();
    }

    @NotNull
    @Override
    public ReceiverValue getReceiverArgument() {
        return variableCall.getReceiverArgument();
    }

    @NotNull
    @Override
    public ReceiverValue getThisObject() {
        return variableCall.getThisObject();
    }

    @NotNull
    @Override
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return variableCall.getExplicitReceiverKind();
    }

    @NotNull
    @Override
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return functionCall.getValueArguments();
    }

    @NotNull
    @Override
    public List<ResolvedValueArgument> getValueArgumentsByIndex() {
        return functionCall.getValueArgumentsByIndex();
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, JetType> getTypeArguments() {
        return functionCall.getTypeArguments();
    }

    @NotNull
    @Override
    public DataFlowInfo getDataFlowInfo() {
        return functionCall.getDataFlowInfo();
    }

    @NotNull
    @Override
    public ResolutionStatus getStatus() {
        if (variableCall.getStatus() == ResolutionStatus.SUCCESS) {
            return functionCall.getStatus();
        }
        return variableCall.getStatus();
    }

    @Override
    public boolean isDirty() {
        return functionCall.isDirty();
    }

    @Override
    public DelegatingBindingTrace getTrace() {
        //functionCall.trace is temporary trace above variableCall.trace and is committed already
        return variableCall.getTrace();
    }

    @Override
    public boolean isSafeCall() {
        return variableCall.isSafeCall();
    }

    @Override
    public boolean hasIncompleteTypeParameters() {
        return variableCall.hasIncompleteTypeParameters();
    }

    @NotNull
    @Override
    public ResolvedCallImpl<FunctionDescriptor> getCallToCompleteTypeArgumentInference() {
        return functionCall.getCallToCompleteTypeArgumentInference();
    }
}
