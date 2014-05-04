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
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public class BasicCallResolutionContext extends CallResolutionContext<BasicCallResolutionContext> {
    private BasicCallResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull CallResolverExtension callResolverExtension,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, callResolverExtension, isAnnotationContext, collectAllCandidates);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull CallResolverExtension callResolverExtension,
            boolean isAnnotationContext
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
             new ResolutionResultsCacheImpl(), null,
             callResolverExtension, isAnnotationContext, false);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext context, @NotNull Call call, @NotNull CheckValueArgumentsMode checkArguments,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new BasicCallResolutionContext(context.trace, context.scope, call, context.expectedType, context.dataFlowInfo, context.contextDependency, checkArguments,
             context.resolutionResultsCache, dataFlowInfoForArguments, context.callResolverExtension,
             context.isAnnotationContext, context.collectAllCandidates);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext context, @NotNull Call call, @NotNull CheckValueArgumentsMode checkArguments
    ) {
        return create(context, call, checkArguments, null);
    }

    @Override
    protected BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            boolean collectAllCandidates
    ) {
        return new BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, callResolverExtension, isAnnotationContext, collectAllCandidates);
    }

    @NotNull
    public BasicCallResolutionContext replaceCall(@NotNull Call newCall) {
        return new BasicCallResolutionContext(
                trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, callResolverExtension, isAnnotationContext, collectAllCandidates);
    }
}
