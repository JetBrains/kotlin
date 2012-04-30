/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.List;
import java.util.Map;

/**
 * @author svtk
 */
public class VariableAsFunctionResolvedCall implements ResolvedCallWithTrace<FunctionDescriptor> {
    private final ResolvedCallWithTrace<FunctionDescriptor> functionCall;
    private final ResolvedCallWithTrace<VariableDescriptor> variableCall;
    private final VariableAsFunctionDescriptor variableAsFunctionDescriptor;

    public VariableAsFunctionResolvedCall(@NotNull ResolvedCallWithTrace<FunctionDescriptor> functionCall,
            @NotNull ResolvedCallWithTrace<VariableDescriptor> variableCall) {
        this.functionCall = functionCall;
        this.variableCall = variableCall;
        if (JetStandardClasses.isFunctionType(variableCall.getResultingDescriptor().getType())) {
            variableAsFunctionDescriptor = VariableAsFunctionDescriptor.create(variableCall.getResultingDescriptor());
        }
        else {
            variableAsFunctionDescriptor = null;
        }
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
        return variableAsFunctionDescriptor != null ? variableAsFunctionDescriptor : functionCall.getResultingDescriptor();
    }

    @NotNull
    @Override
    public FunctionDescriptor getResultingDescriptor() {
        return variableAsFunctionDescriptor != null ? variableAsFunctionDescriptor : functionCall.getResultingDescriptor();
    }

    @NotNull
    @Override
    public ReceiverDescriptor getReceiverArgument() {
        ReceiverDescriptor receiverArgument = variableCall.getReceiverArgument();
        return receiverArgument.exists() ? receiverArgument : functionCall.getReceiverArgument();
    }

    @NotNull
    @Override
    public ReceiverDescriptor getThisObject() {
        return variableCall.getThisObject();
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
    public TemporaryBindingTrace getTrace() {
        //functionCall.trace is temporary trace above variableCall.trace and is committed already
        return variableCall.getTrace();
    }
}
