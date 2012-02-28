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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.List;
import java.util.Map;

/**
* @author abreslav
*/
/*package*/ class ExpressionTypingContext {
    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull Project project,
            @NotNull JetSemanticServices semanticServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType,
            boolean namespacesAllowed) {
        return new ExpressionTypingContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists,
                                           labelResolver, trace, scope, dataFlowInfo, expectedType, expectedReturnType, namespacesAllowed);
    }

//    @NotNull
//    public static ExpressionTypingContext newRootContext(
//            @NotNull JetSemanticServices semanticServices,
//            @NotNull BindingTrace trace,
//            @NotNull JetScope scope,
//            @NotNull DataFlowInfo dataFlowInfo,
//            @NotNull JetType expectedType,
//            @NotNull JetType expectedReturnType) {
//        return newContext(semanticServices, new HashMap<JetPattern, DataFlowInfo>(), new HashMap<JetPattern, List<VariableDescriptor>>(), new LabelResolver(), trace, scope, dataFlowInfo, expectedType, expectedReturnType);
//    }
//
    public final Project project;
    public final JetSemanticServices semanticServices;
    public final BindingTrace trace;
    public final JetScope scope;

    public final DataFlowInfo dataFlowInfo;
    public final JetType expectedType;
    public final JetType expectedReturnType;

    public final Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo;
    public final Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists;
    public final LabelResolver labelResolver;

    // true for positions on the lhs of a '.', i.e. allows namespace results and 'super'
    public final boolean namespacesAllowed;

    private CallResolver callResolver;
    private TypeResolver typeResolver;
    private DescriptorResolver descriptorResolver;
    private ExpressionTypingServices services;
    private CompileTimeConstantResolver compileTimeConstantResolver;

    private ExpressionTypingContext(
            @NotNull Project project,
            @NotNull JetSemanticServices semanticServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType,
            boolean namespacesAllowed) {
        this.project = project;
        this.trace = trace;
        this.patternsToBoundVariableLists = patternsToBoundVariableLists;
        this.patternsToDataFlowInfo = patternsToDataFlowInfo;
        this.labelResolver = labelResolver;
        this.scope = scope;
        this.semanticServices = semanticServices;
        this.dataFlowInfo = dataFlowInfo;
        this.expectedType = expectedType;
        this.expectedReturnType = expectedReturnType;
        this.namespacesAllowed = namespacesAllowed;
    }

    @NotNull
    public ExpressionTypingContext replaceNamespacesAllowed(boolean namespacesAllowed) {
        if (namespacesAllowed == this.namespacesAllowed) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, expectedReturnType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, newDataFlowInfo, expectedType, expectedReturnType, namespacesAllowed);
    }

    public ExpressionTypingContext replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, newExpectedType, expectedReturnType, namespacesAllowed);
    }

    public ExpressionTypingContext replaceExpectedReturnType(@Nullable JetType newExpectedReturnType) {
        if (newExpectedReturnType == null) return replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedReturnType == newExpectedReturnType) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, newExpectedReturnType, namespacesAllowed);
    }

    public ExpressionTypingContext replaceBindingTrace(@NotNull BindingTrace newTrace) {
        if (newTrace == trace) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, newTrace, scope, dataFlowInfo, expectedType, expectedReturnType, namespacesAllowed);
    }

    @NotNull
    public ExpressionTypingContext replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return this;
        return newContext(project, semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, newScope, dataFlowInfo, expectedType, expectedReturnType, namespacesAllowed);
    }

///////////// LAZY ACCESSORS

    public CallResolver getCallResolver() {
        if (callResolver == null) {
            callResolver = new CallResolver(semanticServices, dataFlowInfo);
        }
        return callResolver;
    }

    public ExpressionTypingServices getServices() {
        if (services == null) {
            services = new ExpressionTypingServices(semanticServices, trace);
        }
        return services;
    }

    public TypeResolver getTypeResolver() {
        if (typeResolver == null) {
            typeResolver = new TypeResolver(semanticServices, trace, true);
        }
        return typeResolver;
    }

    public DescriptorResolver getDescriptorResolver() {
        if (descriptorResolver == null) {
            descriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        }
        return descriptorResolver;
    }

    public CompileTimeConstantResolver getCompileTimeConstantResolver() {
        if (compileTimeConstantResolver == null) {
            compileTimeConstantResolver = new CompileTimeConstantResolver(semanticServices, trace);
        }
        return compileTimeConstantResolver;
    }

////////// Call resolution utilities

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull String name) {
        return getCallResolver().resolveCallWithGivenName(trace, scope, call, functionReference, name, expectedType);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenNameToDescriptor(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull String name) {
        return resolveCallWithGivenName(call, functionReference, name);
//        return results == null ? null : results.getResultingDescriptor();
    }

    @NotNull
    public List<FunctionDescriptor> resolveForReceiverOnly(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull String name) {
        return getCallResolver().resolveCallsByReceiver(trace, scope, call, name);
    }


    @Nullable
    public FunctionDescriptor resolveCall(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetCallExpression callExpression) {
        OverloadResolutionResults<FunctionDescriptor> results = getCallResolver().resolveCall(trace, scope, CallMaker.makeCall(receiver, callOperationNode, callExpression), expectedType);
        return results.singleResult() ? results.getResultingDescriptor() : null;
    }

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetSimpleNameExpression nameExpression) {
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        return getCallResolver().resolveSimpleProperty(trace, scope, call, expectedType);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull ReceiverDescriptor receiver, @NotNull String name, @NotNull List<JetType> parameterTypes) {
        return getCallResolver().resolveExactSignature(scope, receiver, name, parameterTypes);
    }

    @NotNull
    public Project getProject() {
        return project;
    }
}
