/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;

public class BasicCallResolutionContext extends CallResolutionContext<BasicCallResolutionContext> {
    private BasicCallResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition,
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
              callPosition, expressionContextProvider);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            boolean isAnnotationContext
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                                              new ResolutionResultsCacheImpl(), null,
                                              StatementFilter.NONE, isAnnotationContext, false, false,
                                              CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext context, @NotNull Call call, @NotNull CheckArgumentTypesMode checkArguments,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new BasicCallResolutionContext(
                context.trace, context.scope, call, context.expectedType, context.dataFlowInfo, context.contextDependency, checkArguments,
                context.resolutionResultsCache, dataFlowInfoForArguments,
                context.statementFilter, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.callPosition, context.expressionContextProvider);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext context, @NotNull Call call, @NotNull CheckArgumentTypesMode checkArguments
    ) {
        return create(context, call, checkArguments, null);
    }

    @Override
    protected BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition,
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider
    ) {
        return new BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                callPosition, expressionContextProvider);
    }

    @NotNull
    public BasicCallResolutionContext replaceCall(@NotNull Call newCall) {
        return new BasicCallResolutionContext(
                trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                callPosition, expressionContextProvider);
    }
}
