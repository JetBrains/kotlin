/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;

public class ExpressionTypingContext extends ResolutionContext<ExpressionTypingContext> {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT, StatementFilter.NONE,
                          languageVersionSettings, dataFlowValueFactory, InferenceSession.Companion.getDefault());
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT, StatementFilter.NONE,
                          languageVersionSettings, dataFlowValueFactory, inferenceSession);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull StatementFilter statementFilter,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory) {
        return newContext(trace, scope, dataFlowInfo, expectedType, contextDependency,
                          new ResolutionResultsCacheImpl(), statementFilter, false, languageVersionSettings, dataFlowValueFactory,
                          InferenceSession.Companion.getDefault());
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull StatementFilter statementFilter,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType, contextDependency,
                          new ResolutionResultsCacheImpl(), statementFilter, false, languageVersionSettings, dataFlowValueFactory,
                          inferenceSession);
    }

    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext<?> context) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.callPosition, context.expressionContextProvider, context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession);
    }

    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext<?> context, boolean isDebuggerContext) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, isDebuggerContext, context.collectAllCandidates,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession);
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
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return new ExpressionTypingContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, false, false,
                CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER, languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
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
            @NotNull CallPosition callPosition,
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
              statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, callPosition, expressionContextProvider,
              languageVersionSettings, dataFlowValueFactory, inferenceSession);
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
            @NotNull CallPosition callPosition,
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return new ExpressionTypingContext(trace, scope, dataFlowInfo,
                                           expectedType, contextDependency, resolutionResultsCache,
                                           statementFilter, isAnnotationContext, isDebuggerContext,
                                           collectAllCandidates, callPosition, expressionContextProvider,
                                           languageVersionSettings, dataFlowValueFactory, inferenceSession);
    }
}
