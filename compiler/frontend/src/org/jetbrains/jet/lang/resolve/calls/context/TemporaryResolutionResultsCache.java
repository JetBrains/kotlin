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
import org.jetbrains.jet.lang.psi.CallKey;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

public class TemporaryResolutionResultsCache implements ResolutionResultsCache {
    private final ResolutionResultsCache parentCache;
    private final ResolutionResultsCacheImpl innerCache;

    public TemporaryResolutionResultsCache(@NotNull ResolutionResultsCache parentCache) {
        assert parentCache instanceof ResolutionResultsCacheImpl || parentCache instanceof TemporaryResolutionResultsCache :
            "Unsupported parent cache: " + parentCache;
        this.parentCache = parentCache;
        this.innerCache = new ResolutionResultsCacheImpl();
    }

    @Override
    public <D extends CallableDescriptor> void recordResolutionResults(
            @NotNull CallKey callKey,
            @NotNull MemberType<D> memberType,
            @NotNull OverloadResolutionResultsImpl<D> results
    ) {
        innerCache.recordResolutionResults(callKey, memberType, results);
    }

    @Nullable
    @Override
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> getResolutionResults(
            @NotNull CallKey callKey, @NotNull MemberType<D> memberType
    ) {
        OverloadResolutionResultsImpl<D> results = innerCache.getResolutionResults(callKey, memberType);
        if (results != null) {
            return results;
        }
        return parentCache.getResolutionResults(callKey, memberType);
    }

    @Override
    public void recordResolutionTrace(
            @NotNull CallKey callKey, @NotNull DelegatingBindingTrace delegatingTrace
    ) {
        innerCache.recordResolutionTrace(callKey, delegatingTrace);
    }

    @Nullable
    @Override
    public DelegatingBindingTrace getResolutionTrace(@NotNull CallKey callKey) {
        DelegatingBindingTrace trace = innerCache.getResolutionTrace(callKey);
        if (trace != null) {
            return trace;
        }
        return parentCache.getResolutionTrace(callKey);
    }

    @Override
    public <D extends CallableDescriptor> void recordDeferredComputationForCall(
            @NotNull CallKey callKey,
            @NotNull CallCandidateResolutionContext<D> deferredComputation
    ) {
        innerCache.recordDeferredComputationForCall(callKey, deferredComputation);
    }

    @Nullable
    @Override
    public CallCandidateResolutionContext<? extends CallableDescriptor> getDeferredComputation(@Nullable JetExpression expression) {
        CallCandidateResolutionContext<? extends CallableDescriptor> computation = innerCache.getDeferredComputation(expression);
        if (computation != null) {
            return computation;
        }
        return parentCache.getDeferredComputation(expression);
    }

    public void commit() {
        if (parentCache instanceof ResolutionResultsCacheImpl) {
            ((ResolutionResultsCacheImpl) parentCache).addData(innerCache);
            return;
        }
        assert parentCache instanceof TemporaryResolutionResultsCache;
        ((TemporaryResolutionResultsCache) parentCache).innerCache.addData(innerCache);
    }
}
