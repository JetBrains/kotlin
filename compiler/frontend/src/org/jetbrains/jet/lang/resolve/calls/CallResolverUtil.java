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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

public class CallResolverUtil {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final JetType CANT_INFER = ErrorUtils.createErrorTypeWithCustomDebugName("CANT_INFER");
    public static final JetType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public enum ResolveMode {
        RESOLVE_FUNCTION_ARGUMENTS,
        SKIP_FUNCTION_ARGUMENTS
    }

    private CallResolverUtil() {}

    public static <D extends CallableDescriptor> ResolvedCallImpl<D> copy(@NotNull ResolvedCallImpl<D> call, @NotNull CallResolutionContext context) {
        ResolutionCandidate<D> candidate = ResolutionCandidate.create(call.getCandidateDescriptor(), call.getThisObject(),
                                                                      call.getReceiverArgument(), call.getExplicitReceiverKind(),
                                                                      call.isSafeCall());

        TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace, call.getTrace().toString() + "(copy)");
        ResolvedCallImpl<D> copy = ResolvedCallImpl.create(candidate, trace);

        call.getTrace().addAllMyDataTo(trace);
        trace.record(BindingContext.RESOLVED_CALL, context.call.getCalleeExpression(), copy);

        copy.addStatus(call.getStatus());
        if (call.isDirty()) {
            copy.argumentHasNoType();
        }
        copy.setHasUnknownTypeParameters(call.hasUnknownTypeParameters());
        ConstraintSystem constraintSystem = call.getConstraintSystem();
        if (constraintSystem != null) {
            copy.setConstraintSystem(constraintSystem.copy());
        }
        for (Map.Entry<TypeParameterDescriptor, JetType> entry : call.getTypeArguments().entrySet()) {
            copy.recordTypeArgument(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet()) {
            copy.recordValueArgument(entry.getKey(), entry.getValue());
        }
        return copy;
    }

}
