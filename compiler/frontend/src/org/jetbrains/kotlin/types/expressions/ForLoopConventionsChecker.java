/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticSink;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.calls.checkers.OperatorCallChecker;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.types.DynamicTypesKt;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collections;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class ForLoopConventionsChecker {
    private final KotlinBuiltIns builtIns;
    private final FakeCallResolver fakeCallResolver;

    public ForLoopConventionsChecker(
            @NotNull KotlinBuiltIns builtIns,
            @NotNull FakeCallResolver fakeCallResolver
    ) {
        this.builtIns = builtIns;
        this.fakeCallResolver = fakeCallResolver;
    }

    @Nullable
    public KotlinType checkIterableConvention(@NotNull ExpressionReceiver loopRange, @NotNull ExpressionTypingContext context) {
        KtExpression loopRangeExpression = loopRange.getExpression();

        // Make a fake call loopRange.iterator(), and try to resolve it
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = fakeCallResolver.resolveFakeCall(
                context, loopRange, OperatorNameConventions.ITERATOR, loopRangeExpression,
                loopRangeExpression, FakeCallKind.ITERATOR, Collections.emptyList()
        );
        if (!iteratorResolutionResults.isSuccess()) return null;

        ResolvedCall<FunctionDescriptor> iteratorResolvedCall = iteratorResolutionResults.getResultingCall();
        context.trace.record(LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRangeExpression, iteratorResolvedCall);
        FunctionDescriptor iteratorFunction = iteratorResolvedCall.getResultingDescriptor();

        checkIfOperatorModifierPresent(loopRangeExpression, iteratorFunction, context.trace);

        KotlinType iteratorType = iteratorFunction.getReturnType();
        //noinspection ConstantConditions
        KotlinType hasNextType = checkConventionForIterator(
                context, loopRangeExpression, iteratorType, OperatorNameConventions.HAS_NEXT,
                HAS_NEXT_FUNCTION_AMBIGUITY, HAS_NEXT_MISSING, HAS_NEXT_FUNCTION_NONE_APPLICABLE, LOOP_RANGE_HAS_NEXT_RESOLVED_CALL
        );
        if (hasNextType != null && !builtIns.isBooleanOrSubtype(hasNextType)) {
            context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRangeExpression, hasNextType));
        }
        return checkConventionForIterator(
                context, loopRangeExpression, iteratorType, OperatorNameConventions.NEXT,
                NEXT_AMBIGUITY, NEXT_MISSING, NEXT_NONE_APPLICABLE, LOOP_RANGE_NEXT_RESOLVED_CALL
        );
    }

    private static void checkIfOperatorModifierPresent(KtExpression expression, FunctionDescriptor descriptor, DiagnosticSink sink) {
        if (ErrorUtils.isError(descriptor)) return;
        ReceiverParameterDescriptor extensionReceiverParameter = descriptor.getExtensionReceiverParameter();
        if ((extensionReceiverParameter != null) && (DynamicTypesKt.isDynamic(extensionReceiverParameter.getType()))) return;

        if (!descriptor.isOperator()) {
            OperatorCallChecker.Companion.report(expression, descriptor, sink);
        }
    }

    @Nullable
    private KotlinType checkConventionForIterator(
            @NotNull ExpressionTypingContext context,
            @NotNull KtExpression loopRangeExpression,
            @NotNull KotlinType iteratorType,
            @NotNull Name name,
            @NotNull DiagnosticFactory1<KtExpression, KotlinType> ambiguity,
            @NotNull DiagnosticFactory1<KtExpression, KotlinType> missing,
            @NotNull DiagnosticFactory1<KtExpression, KotlinType> noneApplicable,
            @NotNull WritableSlice<KtExpression, ResolvedCall<FunctionDescriptor>> resolvedCallKey
    ) {
        OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = fakeCallResolver.resolveFakeCall(
                context, new TransientReceiver(iteratorType), name, loopRangeExpression, loopRangeExpression, FakeCallKind.OTHER,
                Collections.emptyList()
        );
        if (nextResolutionResults.isAmbiguity()) {
            context.trace.report(ambiguity.on(loopRangeExpression, iteratorType));
        }
        else if (nextResolutionResults.isNothing()) {
            context.trace.report(missing.on(loopRangeExpression, iteratorType));
        }
        else if (!nextResolutionResults.isSuccess()) {
            context.trace.report(noneApplicable.on(loopRangeExpression, iteratorType));
        }
        else {
            assert nextResolutionResults.isSuccess();
            ResolvedCall<FunctionDescriptor> resolvedCall = nextResolutionResults.getResultingCall();
            context.trace.record(resolvedCallKey, loopRangeExpression, resolvedCall);

            FunctionDescriptor functionDescriptor = resolvedCall.getResultingDescriptor();
            checkIfOperatorModifierPresent(loopRangeExpression, functionDescriptor, context.trace);

            return functionDescriptor.getReturnType();
        }
        return null;
    }
}
