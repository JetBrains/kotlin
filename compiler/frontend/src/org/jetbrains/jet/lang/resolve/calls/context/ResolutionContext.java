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
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

public abstract class ResolutionContext<Context extends ResolutionContext<Context>> {
    @NotNull
    public final BindingTrace trace;
    @NotNull
    public final JetScope scope;
    @NotNull
    public final JetType expectedType;
    @NotNull
    public final DataFlowInfo dataFlowInfo;
    @NotNull
    public final ContextDependency contextDependency;
    @NotNull
    public final ResolutionResultsCache resolutionResultsCache;
    @NotNull
    public final CallResolverExtension callResolverExtension;

    public final boolean isAnnotationContext;

    public final boolean collectAllCandidates;

    protected ResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull CallResolverExtension callResolverExtension,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.contextDependency = contextDependency;
        this.resolutionResultsCache = resolutionResultsCache;
        this.callResolverExtension = callResolverExtension;
        this.isAnnotationContext = isAnnotationContext;
        this.collectAllCandidates = collectAllCandidates;
    }

    protected abstract Context create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            boolean collectAllCandidates
    );

    @NotNull
    private Context self() {
        //noinspection unchecked
        return (Context) this;
    }

    @NotNull
    public Context replaceBindingTrace(@NotNull BindingTrace trace) {
        if (this.trace == trace) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
        if (newContextDependency == contextDependency) return self();
        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, collectAllCandidates);
    }

    @NotNull
    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }

    @NotNull
    public Context replaceCollectAllCandidates(boolean newCollectAllCandidates) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, newCollectAllCandidates);
    }
}