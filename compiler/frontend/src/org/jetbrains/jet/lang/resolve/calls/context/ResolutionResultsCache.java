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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.CallKey;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

public interface ResolutionResultsCache {
    class MemberType<D extends CallableDescriptor> {
        public final String debugName;

        public MemberType(String name) {
            debugName = name;
        }

        @Override
        public String toString() {
            return debugName;
        }
    }
    MemberType<FunctionDescriptor> FUNCTION_MEMBER_TYPE = new MemberType<FunctionDescriptor>("FUNCTION_MEMBER_TYPE");
    MemberType<VariableDescriptor> PROPERTY_MEMBER_TYPE = new MemberType<VariableDescriptor>("PROPERTY_MEMBER_TYPE");


    <D extends CallableDescriptor> void recordResolutionResults(@NotNull CallKey callKey, @NotNull MemberType<D> memberType, @NotNull OverloadResolutionResultsImpl<D> results);

    @Nullable
    <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> getResolutionResults(@NotNull CallKey callKey, @NotNull MemberType<D> memberType);

    void recordResolutionTrace(@NotNull CallKey callKey, @NotNull DelegatingBindingTrace delegatingTrace);

    @Nullable
    DelegatingBindingTrace getResolutionTrace(@NotNull CallKey callKey);

    <D extends CallableDescriptor> void recordDeferredComputationForCall(
            @NotNull CallKey callKey,
            @NotNull ResolvedCallWithTrace<D> resolvedCall,
            @NotNull CallCandidateResolutionContext<D> deferredComputation
    );

    @Nullable
    CallCandidateResolutionContext<? extends CallableDescriptor> getDeferredComputation(@Nullable JetExpression expression);

    @Nullable
    ResolvedCallWithTrace<? extends CallableDescriptor> getCallForArgument(@Nullable JetExpression expression);
}
