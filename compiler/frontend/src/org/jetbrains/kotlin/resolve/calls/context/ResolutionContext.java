/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.context;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

/**
 * This class together with its descendants is intended to transfer data flow analysis information
 * in top-down direction, from AST parents to children.
 *
 * NB: all descendants must be immutable!
 */
public abstract class ResolutionContext<Context extends ResolutionContext<Context>> {
    @NotNull
    public final BindingTrace trace;
    @NotNull
    public final LexicalScope scope;
    @NotNull
    public final KotlinType expectedType;
    @NotNull
    public final DataFlowInfo dataFlowInfo;
    @NotNull
    public final ContextDependency contextDependency;
    @NotNull
    public final ResolutionResultsCache resolutionResultsCache;
    @NotNull
    public final StatementFilter statementFilter;

    public final boolean isAnnotationContext;

    public final boolean isDebuggerContext;

    public final boolean collectAllCandidates;

    @NotNull
    public final CallPosition callPosition;

    @NotNull
    public final LanguageVersionSettings languageVersionSettings;

    @NotNull
    public final DataFlowValueFactory dataFlowValueFactory;

    @NotNull
    public final InferenceSession inferenceSession;

    /**
     * Used for analyzing expression in the given context.
     * Should be used for going through parents to find containing function, loop etc.
     * The provider should return specific context expression (which can be used instead of parent)
     * for the given expression or null otherwise.
     * @see #getContextParentOfType
     */
    @NotNull
    public final Function1<KtExpression, KtExpression> expressionContextProvider;

    public static final Function1<KtExpression, KtExpression> DEFAULT_EXPRESSION_CONTEXT_PROVIDER = expression -> null;

    protected ResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition,
            @NotNull Function1<KtExpression, KtExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory factory,
            @NotNull InferenceSession inferenceSession
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.contextDependency = contextDependency;
        this.resolutionResultsCache = resolutionResultsCache;
        this.statementFilter = statementFilter;
        this.isAnnotationContext = isAnnotationContext;
        this.isDebuggerContext = isDebuggerContext;
        this.collectAllCandidates = collectAllCandidates;
        this.callPosition = callPosition;
        this.expressionContextProvider = expressionContextProvider;
        this.languageVersionSettings = languageVersionSettings;
        this.dataFlowValueFactory = factory;
        this.inferenceSession = inferenceSession;
    }

    protected abstract Context create(
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
    );

    @NotNull
    @SuppressWarnings("unchecked")
    private Context self() {
        return (Context) this;
    }

    @NotNull
    public Context replaceBindingTrace(@NotNull BindingTrace trace) {
        if (this.trace == trace) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceInferenceSession(@NotNull InferenceSession newInferenceSession) {
        if (newInferenceSession == inferenceSession) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      newInferenceSession);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable KotlinType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceScope(@NotNull LexicalScope newScope) {
        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
        if (newContextDependency == contextDependency) return self();
        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }

    @NotNull
    public Context replaceCollectAllCandidates(boolean newCollectAllCandidates) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      newCollectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceStatementFilter(@NotNull StatementFilter statementFilter) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceCallPosition(@NotNull CallPosition callPosition) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @NotNull
    public Context replaceExpressionContextProvider(@NotNull Function1<KtExpression, KtExpression> expressionContextProvider) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                      collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                      inferenceSession);
    }

    @Nullable
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends PsiElement> T getContextParentOfType(@NotNull KtExpression expression, @NotNull Class<? extends T>... classes) {
        KtExpression context = expressionContextProvider.invoke(expression);
        PsiElement current = context != null ? context : expression.getParent();

        while (current != null) {
            for (Class<? extends T> klass : classes) {
                if (klass.isInstance(current)) {
                    return (T) current;
                }
            }

            if (current instanceof PsiFile) return null;

            if (current instanceof KtExpression) {
                context = expressionContextProvider.invoke((KtExpression) current);
                if (context != null) {
                    current = context;
                    continue;
                }
            }

            current = current.getParent();
        }
        return null;
    }
}
