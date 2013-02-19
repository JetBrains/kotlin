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

package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.ExpressionPosition;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.ResolveMode;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public class ExpressionTypingContext extends ResolutionContext<ExpressionTypingContext> {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            ExpressionPosition expressionPosition) {
        return newContext(expressionTypingServices, new LabelResolver(), trace, scope, dataFlowInfo, expectedType, expressionPosition);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            ExpressionPosition expressionPosition) {
        return new ExpressionTypingContext(expressionTypingServices, 
                                           labelResolver, trace, scope, dataFlowInfo, expectedType, expressionPosition);
    }

    public final ExpressionTypingServices expressionTypingServices;

    public final LabelResolver labelResolver;

    private CompileTimeConstantResolver compileTimeConstantResolver;

    private ExpressionTypingContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            ExpressionPosition expressionPosition) {
        super(trace, scope, expectedType, dataFlowInfo, expressionPosition);
        this.expressionTypingServices = expressionTypingServices;
        this.labelResolver = labelResolver;
    }

    @Override
    protected ExpressionTypingContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            ExpressionPosition expressionPosition
    ) {
        return new ExpressionTypingContext(expressionTypingServices, labelResolver, trace, scope, dataFlowInfo, expectedType, expressionPosition);
    }

    @Override
    protected ExpressionTypingContext self() {
        return this;
    }

///////////// LAZY ACCESSORS

    public CompileTimeConstantResolver getCompileTimeConstantResolver() {
        if (compileTimeConstantResolver == null) {
            compileTimeConstantResolver = new CompileTimeConstantResolver();
        }
        return compileTimeConstantResolver;
    }

////////// Call resolution utilities

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull Name name) {
        return expressionTypingServices.getCallResolver().resolveCallWithGivenName(
                BasicCallResolutionContext.create(this, call, ResolveMode.TOP_LEVEL_CALL), functionReference, name);
    }
}
