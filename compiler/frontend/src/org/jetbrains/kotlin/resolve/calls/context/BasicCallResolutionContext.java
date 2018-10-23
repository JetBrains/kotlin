/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.context;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
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
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
              callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory, inferenceSession);
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
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                                              new ResolutionResultsCacheImpl(), null,
                                              StatementFilter.NONE, isAnnotationContext, false, false,
                                              CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER, languageVersionSettings,
                                              dataFlowValueFactory, inferenceSession);
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
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                                              new ResolutionResultsCacheImpl(), null,
                                              StatementFilter.NONE, isAnnotationContext, false, false,
                                              CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER, languageVersionSettings,
                                              dataFlowValueFactory, InferenceSession.Companion.getDefault());
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext<?> context, @NotNull Call call, @NotNull CheckArgumentTypesMode checkArguments,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new BasicCallResolutionContext(
                context.trace, context.scope, call, context.expectedType, context.dataFlowInfo, context.contextDependency, checkArguments,
                context.resolutionResultsCache, dataFlowInfoForArguments,
                context.statementFilter, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.callPosition, context.expressionContextProvider, context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession);
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
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return new BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory, inferenceSession);
    }

    @NotNull
    public BasicCallResolutionContext replaceCall(@NotNull Call newCall) {
        return new BasicCallResolutionContext(
                trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory, inferenceSession);
    }
}
