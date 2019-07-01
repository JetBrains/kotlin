/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;

public abstract class CallResolutionContext<Context extends CallResolutionContext<Context>> extends ResolutionContext<Context> {
    @NotNull
    public final Call call;
    @NotNull
    public final CheckArgumentTypesMode checkArguments;
    @NotNull
    public final MutableDataFlowInfoForArguments dataFlowInfoForArguments;

    protected CallResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @SuppressWarnings("NullableProblems")
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
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
              statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings,
              dataFlowValueFactory, inferenceSession);
        this.call = call;
        this.checkArguments = checkArguments;
        if (dataFlowInfoForArguments != null) {
            this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        }
        else if (checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            this.dataFlowInfoForArguments = new DataFlowInfoForArgumentsImpl(dataFlowInfo, call);
        }
        else {
            this.dataFlowInfoForArguments = new MutableDataFlowInfoForArguments.WithoutArgumentsCheck(dataFlowInfo);
        }
    }
}
