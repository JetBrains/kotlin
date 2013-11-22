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

package org.jetbrains.jet.lang.resolve.calls.context;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.CallKey;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.util.slicedmap.BasicWritableSlice;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import static org.jetbrains.jet.lang.psi.Call.CallType;
import static org.jetbrains.jet.lang.psi.Call.CallType.*;

public class ResolutionResultsCacheImpl implements ResolutionResultsCache {
    public static final WritableSlice<CallKey, OverloadResolutionResultsImpl<FunctionDescriptor>>
            RESOLUTION_RESULTS_FOR_FUNCTION = Slices.createSimpleSlice();
    public static final WritableSlice<CallKey, OverloadResolutionResultsImpl<VariableDescriptor>> RESOLUTION_RESULTS_FOR_PROPERTY = Slices.createSimpleSlice();
    public static final WritableSlice<CallKey, DelegatingBindingTrace> TRACE_DELTAS_CACHE = Slices.createSimpleSlice();
    public static final WritableSlice<CallKey, CallCandidateResolutionContext<? extends CallableDescriptor>> DEFERRED_COMPUTATION_FOR_CALL = Slices.createSimpleSlice();
    public static final WritableSlice<CallKey, ResolvedCallWithTrace<? extends CallableDescriptor>> RESOLVED_CALL_FOR_ARGUMENT = Slices.createSimpleSlice();

    static {
        BasicWritableSlice.initSliceDebugNames(ResolutionResultsCacheImpl.class);
    }

    private final DelegatingBindingTrace trace = new DelegatingBindingTrace(
            BindingContext.EMPTY, "Internal binding context in resolution results cache");

    @NotNull
    private static <D extends CallableDescriptor> WritableSlice<CallKey, OverloadResolutionResultsImpl<D>> getSliceByMemberType(@NotNull MemberType<D> memberType) {
        //noinspection unchecked
        return (WritableSlice<CallKey, OverloadResolutionResultsImpl<D>>)
                (memberType == FUNCTION_MEMBER_TYPE ? RESOLUTION_RESULTS_FOR_FUNCTION : RESOLUTION_RESULTS_FOR_PROPERTY);
    }

    @Override
    public <D extends CallableDescriptor> void recordResolutionResults(@NotNull CallKey callKey, @NotNull MemberType<D> memberType, @NotNull OverloadResolutionResultsImpl<D> results) {
        trace.record(getSliceByMemberType(memberType), callKey, results);
    }

    @Override
    @Nullable
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> getResolutionResults(@NotNull CallKey callKey, @NotNull MemberType<D> memberType) {
        return trace.get(getSliceByMemberType(memberType), callKey);
    }

    @Override
    public void recordResolutionTrace(@NotNull CallKey callKey, @NotNull DelegatingBindingTrace delegatingTrace) {
        trace.record(TRACE_DELTAS_CACHE, callKey, delegatingTrace);
    }

    @Override
    @Nullable
    public DelegatingBindingTrace getResolutionTrace(@NotNull CallKey callKey) {
        return trace.get(TRACE_DELTAS_CACHE, callKey);
    }

    @Override
    public <D extends CallableDescriptor> void recordDeferredComputationForCall(
            @NotNull CallKey callKey,
            @NotNull ResolvedCallWithTrace<D> resolvedCall,
            @NotNull CallCandidateResolutionContext<D> deferredComputation
    ) {
        trace.record(DEFERRED_COMPUTATION_FOR_CALL, callKey, deferredComputation);
        trace.record(RESOLVED_CALL_FOR_ARGUMENT, callKey, resolvedCall);
    }

    @Override
    @Nullable
    public CallCandidateResolutionContext<? extends CallableDescriptor> getDeferredComputation(@Nullable JetExpression expression) {
        return getValueTryingAllCallTypes(expression, DEFERRED_COMPUTATION_FOR_CALL);
    }

    @Nullable
    private <T> T getValueTryingAllCallTypes(
            @Nullable JetExpression expression,
            @NotNull WritableSlice<CallKey, T> slice
    ) {
        if (expression == null) return null;
        for (CallType callType : Lists.newArrayList(DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD)) {
            CallKey callKey = CallKey.create(callType, expression);
            T context = trace.get(slice, callKey);
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public ResolvedCallWithTrace<? extends CallableDescriptor> getCallForArgument(@Nullable JetExpression expression) {
        return getValueTryingAllCallTypes(expression, RESOLVED_CALL_FOR_ARGUMENT);
    }

    @NotNull
    public static ResolutionResultsCache create() {
        return new ResolutionResultsCacheImpl();
    }

    /*package*/ void addData(@NotNull ResolutionResultsCacheImpl cache) {
        cache.trace.addAllMyDataTo(this.trace);
    }
}
