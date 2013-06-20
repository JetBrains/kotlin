package org.jetbrains.jet.lang.resolve.calls.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public class SimpleResolutionContext extends ResolutionContext<SimpleResolutionContext> {
    public SimpleResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ResolveMode resolveMode,
            @NotNull ResolutionResultsCache resolutionResultsCache
    ) {
        super(trace, scope, expectedType, dataFlowInfo, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @Override
    protected SimpleResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ResolveMode resolveMode,
            @NotNull ResolutionResultsCache resolutionResultsCache
    ) {
        return new SimpleResolutionContext(trace, scope, expectedType, dataFlowInfo, expressionPosition, resolveMode, resolutionResultsCache);
    }

    @Override
    protected SimpleResolutionContext self() {
        return this;
    }
}
