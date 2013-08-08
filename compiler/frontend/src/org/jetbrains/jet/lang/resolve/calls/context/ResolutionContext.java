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
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

public abstract class ResolutionContext<Context extends ResolutionContext<Context>> {
    public final BindingTrace trace;
    public final JetScope scope;
    public final JetType expectedType;
    public final DataFlowInfo dataFlowInfo;
    public final ExpressionPosition expressionPosition;
    public final ResolveMode resolveMode;
    public final ResolutionResultsCache resolutionResultsCache;

    protected ResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ResolveMode resolveMode,
            @NotNull ResolutionResultsCache resolutionResultsCache
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.expressionPosition = expressionPosition;
        this.resolveMode = resolveMode;
        this.resolutionResultsCache = resolutionResultsCache;
    }

    protected abstract Context create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ResolveMode resolveMode,
            @NotNull ResolutionResultsCache resolutionResultsCache
    );
    
    protected abstract Context self();
    
    public Context replaceBindingTrace(@NotNull BindingTrace trace) {
        if (this.trace == trace) return self();
        return create(trace, scope, dataFlowInfo, expectedType, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceExpressionPosition(@NotNull ExpressionPosition expressionPosition) {
        if (expressionPosition == this.expressionPosition) return self();
        return create(trace, scope, dataFlowInfo, expectedType, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceResolveMode(@NotNull ResolveMode newResolveMode) {
        if (newResolveMode == resolveMode) return self();
        return create(trace, scope, dataFlowInfo, expectedType, expressionPosition, newResolveMode, resolutionResultsCache);
    }

    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, expressionPosition, resolveMode, newResolutionResultsCache);
    }

    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }
}