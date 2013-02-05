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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallResolverUtil {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final JetType CANT_INFER = ErrorUtils.createErrorTypeWithCustomDebugName("CANT_INFER");
    public static final JetType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public static enum ResolveArgumentsMode {
        RESOLVE_FUNCTION_ARGUMENTS,
        SKIP_FUNCTION_ARGUMENTS
    }

    private CallResolverUtil() {}

    public static <D extends CallableDescriptor> ResolvedCallImpl<D> copy(@NotNull ResolvedCallImpl<D> call, @NotNull CallResolutionContext context) {
        ResolutionCandidate<D> candidate = ResolutionCandidate.create(call.getCandidateDescriptor(), call.getThisObject(),
                                                                      call.getReceiverArgument(), call.getExplicitReceiverKind(),
                                                                      call.isSafeCall());

        TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace, call.getTrace().toString() + "(copy)");
        ResolvedCallImpl<D> copy = ResolvedCallImpl.create(candidate, trace, call.getTracing());

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
        copy.setInitialDataFlowInfo(call.getDataFlowInfo());
        for (ResolvedValueArgument resolvedArgument : call.getValueArguments().values()) {
            for (ValueArgument argument : resolvedArgument.getArguments()) {
                copy.addDeferredComputationForArgument(argument, call.getDeferredComputationForArgument(argument));
            }
        }
        return copy;
    }


    public static boolean containsUnknownFunctionArgument(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        int index = 0;
        for (TypeProjection argument : arguments) {
            if (index < arguments.size() - 1) {
                if (ErrorUtils.containsErrorType(argument.getType())) {
                    return true;
                }
            }
            index++;
        }
        return false;
    }

    public static boolean hasUnknownReturnType(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        JetType returnTypeFromFunctionType = KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(type);
        return ErrorUtils.containsErrorType(returnTypeFromFunctionType);
    }

    public static JetType replaceReturnTypeToUnknown(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        List<TypeProjection> newArguments = Lists.newArrayList();
        int index = 0;
        for (TypeProjection argument : arguments) {
            if (index < arguments.size() - 1) {
                newArguments.add(argument);
            }
            index++;
        }
        newArguments.add(new TypeProjection(Variance.INVARIANT, DONT_CARE));
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberScope());
    }

    public static <D extends CallableDescriptor> boolean hasReturnTypeDependentOnNotInferredParams(@NotNull ResolvedCall<D> resolvedCall) {
        //todo[ResolvedCallImpl]
        if (!(resolvedCall instanceof ResolvedCallImpl)) return false;
        ResolvedCallImpl call = (ResolvedCallImpl) resolvedCall;
        ConstraintSystem constraintSystem = call.getConstraintSystem();
        if (constraintSystem == null) return false;

        CallableDescriptor candidateDescriptor = call.getCandidateDescriptor();
        JetType returnType = candidateDescriptor.getReturnType();
        if (returnType == null) return false;

        for (TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables()) {
            JetType inferredValueForTypeVariable = ConstraintsUtil.getValue(constraintSystem.getTypeConstraints(typeVariable));
            if (inferredValueForTypeVariable == null) {
                if (TypeUtils.dependsOnTypeParameters(returnType, Collections.singleton(typeVariable))) {
                    return true;
                }
            }
        }
        return false;
    }
}
