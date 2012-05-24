/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.BasicResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* @author abreslav
*/
public class ExpressionTypingContext {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed) {
        return new ExpressionTypingContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists,
                                           labelResolver, trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    public final ExpressionTypingServices expressionTypingServices;
    public final BindingTrace trace;
    public final JetScope scope;

    public final DataFlowInfo dataFlowInfo;
    public final JetType expectedType;

    public final Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo;
    public final Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists;
    public final LabelResolver labelResolver;

    // true for positions on the lhs of a '.', i.e. allows namespace results and 'super'
    public final boolean namespacesAllowed;

    private CompileTimeConstantResolver compileTimeConstantResolver;

    private ExpressionTypingContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed) {
        this.expressionTypingServices = expressionTypingServices;
        this.trace = trace;
        this.patternsToBoundVariableLists = patternsToBoundVariableLists;
        this.patternsToDataFlowInfo = patternsToDataFlowInfo;
        this.labelResolver = labelResolver;
        this.scope = scope;
        this.dataFlowInfo = dataFlowInfo;
        this.expectedType = expectedType;
        this.namespacesAllowed = namespacesAllowed;
    }

    @NotNull
    public ExpressionTypingContext replaceNamespacesAllowed(boolean namespacesAllowed) {
        if (namespacesAllowed == this.namespacesAllowed) return this;
        return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return this;
        return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, newDataFlowInfo, expectedType, namespacesAllowed);
    }

    public ExpressionTypingContext replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return this;
        return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, newExpectedType, namespacesAllowed);
    }

    public ExpressionTypingContext replaceBindingTrace(@NotNull BindingTrace newTrace) {
        if (newTrace == trace) return this;
        return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, newTrace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return this;
        return newContext(expressionTypingServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, newScope, dataFlowInfo, expectedType, namespacesAllowed);
    }

///////////// LAZY ACCESSORS

    public CompileTimeConstantResolver getCompileTimeConstantResolver() {
        if (compileTimeConstantResolver == null) {
            compileTimeConstantResolver = new CompileTimeConstantResolver(trace);
        }
        return compileTimeConstantResolver;
    }

////////// Call resolution utilities

    private BasicResolutionContext makeResolutionContext(@NotNull Call call) {
        return BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo);
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
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetSimpleNameExpression nameExpression) {
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        return expressionTypingServices.getCallResolver().resolveSimpleProperty(makeResolutionContext(call));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull ReceiverDescriptor receiver, @NotNull Name name, @NotNull List<JetType> parameterTypes) {
        return expressionTypingServices.getCallResolver().resolveExactSignature(scope, receiver, name, parameterTypes);
    }
}
