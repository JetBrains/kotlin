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

package org.jetbrains.kotlin.resolve.calls.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

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
    public final CallChecker callChecker;
    @NotNull
    public final StatementFilter statementFilter;

    public final AdditionalTypeChecker additionalTypeChecker;

    public final boolean isAnnotationContext;

    public final boolean collectAllCandidates;

    protected ResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull CallChecker callChecker,
            @NotNull AdditionalTypeChecker additionalTypeChecker,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.contextDependency = contextDependency;
        this.resolutionResultsCache = resolutionResultsCache;
        this.callChecker = callChecker;
        this.statementFilter = statementFilter;
        this.additionalTypeChecker = additionalTypeChecker;
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
            @NotNull StatementFilter statementFilter,
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
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
        if (newContextDependency == contextDependency) return self();
        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }

    @NotNull
    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }

    @NotNull
    public Context replaceCollectAllCandidates(boolean newCollectAllCandidates) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      newCollectAllCandidates);
    }

    @NotNull
    public Context replacestatementFilter(@NotNull StatementFilter statementFilter) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates);
    }
}
