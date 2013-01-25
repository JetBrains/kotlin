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

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.BasicResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.List;

public class ExpressionTypingContext {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed) {
        return newContext(expressionTypingServices, new LabelResolver(), trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed) {
        return new ExpressionTypingContext(expressionTypingServices, 
                                           labelResolver, trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    public final ExpressionTypingServices expressionTypingServices;
    public final BindingTrace trace;
    public final JetScope scope;

    public final DataFlowInfo dataFlowInfo;
    public final JetType expectedType;

    public final LabelResolver labelResolver;

    // true for positions on the lhs of a '.', i.e. allows namespace results and 'super'
    public final boolean namespacesAllowed;

    private CompileTimeConstantResolver compileTimeConstantResolver;

    private ExpressionTypingContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed) {
        this.expressionTypingServices = expressionTypingServices;
        this.trace = trace;
        this.labelResolver = labelResolver;
        this.scope = scope;
        this.dataFlowInfo = dataFlowInfo;
        this.expectedType = expectedType;
        this.namespacesAllowed = namespacesAllowed;
    }

    @NotNull
    public ExpressionTypingContext replaceNamespacesAllowed(boolean namespacesAllowed) {
        if (namespacesAllowed == this.namespacesAllowed) return this;
        return newContext(expressionTypingServices, labelResolver,
                          trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return this;
        return newContext(expressionTypingServices, labelResolver,
                          trace, scope, newDataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return this;
        return newContext(expressionTypingServices, labelResolver,
                          trace, scope, dataFlowInfo, newExpectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceBindingTrace(@NotNull BindingTrace newTrace) {
        if (newTrace == trace) return this;
        return newContext(expressionTypingServices, labelResolver,
                          newTrace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return this;
        return newContext(expressionTypingServices, labelResolver,
                          trace, newScope, dataFlowInfo, expectedType, namespacesAllowed);
    }

///////////// LAZY ACCESSORS

    public CompileTimeConstantResolver getCompileTimeConstantResolver() {
        if (compileTimeConstantResolver == null) {
            compileTimeConstantResolver = new CompileTimeConstantResolver();
        }
        return compileTimeConstantResolver;
    }

////////// Call resolution utilities

    private BasicResolutionContext makeResolutionContext(@NotNull Call call) {
        return BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo, namespacesAllowed);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull Name name) {
        return expressionTypingServices.getCallResolver().resolveCallWithGivenName(makeResolutionContext(call), functionReference, name);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull Call call) {
        return expressionTypingServices.getCallResolver().resolveFunctionCall(makeResolutionContext(call));
    }

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull ReceiverValue receiver, @Nullable ASTNode callOperationNode, @NotNull JetSimpleNameExpression nameExpression) {
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        return expressionTypingServices.getCallResolver().resolveSimpleProperty(makeResolutionContext(call));
    }
}
