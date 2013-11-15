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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.CallKey;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TraceUtil;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.CANT_INFER_TYPE_PARAMETER;
import static org.jetbrains.jet.lang.types.TypeUtils.DONT_CARE;

public class CallResolverUtil {
    public static enum ResolveArgumentsMode {
        RESOLVE_FUNCTION_ARGUMENTS,
        SHAPE_FUNCTION_ARGUMENTS
    }

    private CallResolverUtil() {}

    public static <D extends CallableDescriptor> ResolvedCallImpl<D> copy(@NotNull ResolvedCallImpl<D> call, @NotNull CallResolutionContext<?> context) {
        ResolutionCandidate<D> candidate = ResolutionCandidate.create(call.getCandidateDescriptor(), call.getThisObject(),
                                                                      call.getReceiverArgument(), call.getExplicitReceiverKind(),
                                                                      call.isSafeCall());

        ResolvedCallImpl<D> copy = ResolvedCallImpl.create(candidate, TraceUtil.DELEGATING_TRACE_STUB, call.getTracing(), call.getDataFlowInfoForArguments());
        context.trace.record(BindingContext.RESOLVED_CALL, context.call.getCalleeExpression(), copy);

        copy.addStatus(call.getStatus());
        if (call.isDirty()) {
            copy.argumentHasNoType();
        }
        copy.setHasUnknownTypeParameters(call.hasIncompleteTypeParameters());
        ConstraintSystem constraintSystem = call.getConstraintSystem();
        if (constraintSystem != null) {
            copy.setConstraintSystem(constraintSystem.copy());
        }
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet()) {
            copy.recordValueArgument(entry.getKey(), entry.getValue());
        }
        return copy;
    }


    public static boolean hasUnknownFunctionParameter(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        // last argument is return type of function type
        List<TypeProjection> functionParameters = arguments.subList(0, arguments.size() - 1);
        for (TypeProjection functionParameter : functionParameters) {
            if (TypeUtils.equalsOrContainsAsArgument(functionParameter.getType(), CANT_INFER_TYPE_PARAMETER, DONT_CARE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUnknownReturnType(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        JetType returnTypeFromFunctionType = KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(type);
        return ErrorUtils.containsErrorType(returnTypeFromFunctionType);
    }

    public static JetType replaceReturnTypeByUnknown(@NotNull JetType type) {
        assert KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        List<TypeProjection> newArguments = Lists.newArrayList();
        newArguments.addAll(arguments.subList(0, arguments.size() - 1));
        newArguments.add(new TypeProjectionImpl(Variance.INVARIANT, DONT_CARE));
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberScope());
    }

    private static boolean hasReturnTypeDependentOnNotInferredParams(
            @NotNull CallableDescriptor candidateDescriptor,
            @NotNull ConstraintSystem constraintSystem
    ) {
        JetType returnType = candidateDescriptor.getReturnType();
        if (returnType == null) return false;

        for (TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables()) {
            JetType inferredValueForTypeVariable = constraintSystem.getTypeBounds(typeVariable).getValue();
            if (inferredValueForTypeVariable == null) {
                if (TypeUtils.dependsOnTypeParameters(returnType, Collections.singleton(typeVariable))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasInferredReturnType(
            @NotNull CallableDescriptor candidateDescriptor,
            @NotNull ConstraintSystem constraintSystem
    ) {
        if (hasReturnTypeDependentOnNotInferredParams(candidateDescriptor, constraintSystem)) return false;

        // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
        if (constraintSystem.getStatus().hasOnlyErrorsFromPosition(ConstraintPosition.EXPECTED_TYPE_POSITION)) return false;
        return true;
    }

    @Nullable
    public static CallKey createCallKey(@NotNull BasicCallResolutionContext context) {
        if (context.call.getCallType() == Call.CallType.INVOKE) {
            return null;
        }
        PsiElement callElement = context.call.getCallElement();
        if (!(callElement instanceof JetExpression)) return null;
        return CallKey.create(context.call.getCallType(), (JetExpression) callElement);
    }

    @NotNull
    public static JetType getErasedReceiverType(
            @NotNull ReceiverParameterDescriptor receiverParameterDescriptor,
            @NotNull CallableDescriptor descriptor
    ) {
        JetType receiverType = receiverParameterDescriptor.getType();
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            if (typeParameter.getTypeConstructor().equals(receiverType.getConstructor())) {
                receiverType = typeParameter.getUpperBoundsAsType();
            }
        }
        List<TypeProjection> fakeTypeArguments = Lists.newArrayList();
        for (TypeProjection typeProjection : receiverType.getArguments()) {
            fakeTypeArguments.add(new TypeProjectionImpl(typeProjection.getProjectionKind(), DONT_CARE));
        }
        return new JetTypeImpl(
                receiverType.getAnnotations(), receiverType.getConstructor(), receiverType.isNullable(),
                fakeTypeArguments, ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true));
    }

    public static boolean isOrOverridesSynthesized(@NotNull CallableMemberDescriptor descriptor) {
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            return true;
        }
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            for (CallableMemberDescriptor overridden : descriptor.getOverriddenDescriptors()) {
                if (!isOrOverridesSynthesized(overridden)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
