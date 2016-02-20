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

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;

public class ExpressionTypingContext extends ResolutionContext<ExpressionTypingContext> {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType,
                          ContextDependency.INDEPENDENT, new ResolutionResultsCacheImpl(),
                          StatementFilter.NONE, false);
    }

    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext context) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.callPosition);
    }

    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext context, boolean isDebuggerContext) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, isDebuggerContext, context.collectAllCandidates,
                context.callPosition);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext
    ) {
        return new ExpressionTypingContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, false, false, CallPosition.Unknown.INSTANCE);
    }

    private ExpressionTypingContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition
    ) {
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
              statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, callPosition);
    }

    @Override
    protected ExpressionTypingContext create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition
    ) {
        return new ExpressionTypingContext(trace, scope, dataFlowInfo,
                                           expectedType, contextDependency, resolutionResultsCache,
                                           statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, callPosition);
    }
}
