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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.CallKey;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.TraceUtil;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.calls.CallTransformer.CallForImplicitInvoke;

public class CallResolverUtil {

    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final JetType CANT_INFER_TYPE_PARAMETER = ErrorUtils.createErrorTypeWithCustomDebugName("CANT_INFER_TYPE_PARAMETER");
    public static final JetType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public static enum ResolveArgumentsMode {
        RESOLVE_FUNCTION_ARGUMENTS,
        SKIP_FUNCTION_ARGUMENTS
    }

    private CallResolverUtil() {}

    public static <D extends CallableDescriptor> ResolvedCallImpl<D> copy(@NotNull ResolvedCallImpl<D> call, @NotNull CallResolutionContext<?> context) {
        ResolutionCandidate<D> candidate = ResolutionCandidate.create(call.getCandidateDescriptor(), call.getThisObject(),
                                                                      call.getReceiverArgument(), call.getExplicitReceiverKind(),
                                                                      call.isSafeCall());

        ResolvedCallImpl<D> copy = ResolvedCallImpl.create(candidate, TraceUtil.DELEGATING_TRACE_STUB, call.getTracing());
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
        for (Map.Entry<TypeParameterDescriptor, JetType> entry : call.getTypeArguments().entrySet()) {
            copy.recordTypeArgument(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet()) {
            copy.recordValueArgument(entry.getKey(), entry.getValue());
        }
        copy.setInitialDataFlowInfo(call.getDataFlowInfo());
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
        newArguments.add(new TypeProjection(Variance.INVARIANT, DONT_CARE));
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), type.isNullable(), newArguments, type.getMemberScope());
    }

    private static boolean hasReturnTypeDependentOnNotInferredParams(@NotNull ResolvedCallImpl<?> callToComplete) {
        ConstraintSystem constraintSystem = callToComplete.getConstraintSystem();
        if (constraintSystem == null) return false;

        CallableDescriptor candidateDescriptor = callToComplete.getCandidateDescriptor();
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

    public static boolean hasInferredReturnType(ResolvedCallWithTrace<?> call) {
        ResolvedCallImpl<?> callToComplete = call.getCallToCompleteTypeArgumentInference();
        if (hasReturnTypeDependentOnNotInferredParams(callToComplete)) return false;

        // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
        ConstraintSystem constraintSystem = callToComplete.getConstraintSystem();
        if (constraintSystem != null && constraintSystem.hasOnlyExpectedTypeMismatch()) return false;
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
}
