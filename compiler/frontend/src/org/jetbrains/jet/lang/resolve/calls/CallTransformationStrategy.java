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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author svtk
 */
public interface CallTransformationStrategy<D extends CallableDescriptor, R extends D> {

    @NotNull
    CallResolutionContext<D> createCallContext(@NotNull ResolutionCandidate<D> candidate,
            @NotNull ResolutionTask<D> task,
            @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing);

    @NotNull
    Collection<ResolvedCallImpl<R>> transformResultCall(@NotNull CallResolutionContext<D> callResolutionContext,
            @NotNull CallResolver callResolver,
            @NotNull ResolutionTask<D> task);

    CallTransformationStrategy<VariableDescriptor, VariableDescriptor>
            PROPERTY_CALL_TRANSFORMATION_STRATEGY = new CallTransformationStrategy<VariableDescriptor, VariableDescriptor>() {
        @NotNull
        @Override
        public CallResolutionContext<VariableDescriptor> createCallContext(@NotNull ResolutionCandidate<VariableDescriptor> candidate,
                @NotNull ResolutionTask<VariableDescriptor> task, @NotNull BindingTrace trace, @NotNull TracingStrategy tracing) {
            ResolvedCallImpl<VariableDescriptor> candidateCall = ResolvedCallImpl.create(candidate);
            return new CallResolutionContext<VariableDescriptor>(candidateCall, task, trace, tracing);
        }

        @NotNull
        @Override
        public Collection<ResolvedCallImpl<VariableDescriptor>> transformResultCall(@NotNull CallResolutionContext<VariableDescriptor> context,
                @NotNull CallResolver callResolver, @NotNull ResolutionTask<VariableDescriptor> task) {
            return Collections.singleton(context.candidateCall);
        }
    };

    CallTransformationStrategy<FunctionDescriptor, FunctionDescriptor>
            FUNCTION_CALL_TRANSFORMATION_STRATEGY = new CallTransformationStrategy<FunctionDescriptor, FunctionDescriptor>() {
        @NotNull
        @Override
        public CallResolutionContext<FunctionDescriptor> createCallContext(@NotNull ResolutionCandidate<FunctionDescriptor> candidate,
                @NotNull ResolutionTask<FunctionDescriptor> task,
                @NotNull BindingTrace trace,
                @NotNull TracingStrategy tracing) {
            if (candidate.getDescriptor() instanceof FunctionDescriptor) {
                return new CallResolutionContext<FunctionDescriptor>(ResolvedCallImpl.create(candidate), task, trace, tracing);
            }
            assert candidate.getDescriptor() instanceof VariableDescriptor;
            Call propertyCall = new DelegatingCall(task.call) {
                @Override
                public JetValueArgumentList getValueArgumentList() {
                    return null;
                }

                @NotNull
                @Override
                public List<JetExpression> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public List<JetTypeProjection> getTypeArguments() {
                    return Collections.emptyList();
                }

                @Override
                public JetTypeArgumentList getTypeArgumentList() {
                    return null;
                }
            };
            return new CallResolutionContext<FunctionDescriptor>(ResolvedCallImpl.create(candidate), task, trace, tracing, propertyCall);
        }

        @NotNull
        @Override
        public Collection<ResolvedCallImpl<FunctionDescriptor>> transformResultCall(@NotNull CallResolutionContext<FunctionDescriptor> context,
                @NotNull CallResolver callResolver, @NotNull ResolutionTask<FunctionDescriptor> task) {
            FunctionDescriptor descriptor = context.candidateCall.getCandidateDescriptor();
            if (descriptor instanceof FunctionDescriptor) {
                return Collections.singleton(context.candidateCall);
            }
            assert descriptor instanceof VariableDescriptor;
            BasicResolutionContext basicResolutionContext =
                    BasicResolutionContext.create(context.trace, context.scope, task.call, context.expectedType, context.dataFlowInfo);
            OverloadResolutionResults<FunctionDescriptor> results =
                    callResolver.resolveCallWithGivenName(basicResolutionContext, task.reference, "invoke");
            return ((OverloadResolutionResultsImpl<FunctionDescriptor>)results).getResultingCalls();
        }
    };
}
