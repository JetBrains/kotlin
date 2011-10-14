package org.jetbrains.jet.lang.types.expressions;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author abreslav
*/
/*package*/ class ExpressionTypingContext {
    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull JetSemanticServices semanticServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType) {
        return new ExpressionTypingContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, expectedReturnType);
    }

    @NotNull
    public static ExpressionTypingContext newRootContext(
            @NotNull JetSemanticServices semanticServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType) {
        return new ExpressionTypingContext(semanticServices, new HashMap<JetPattern, DataFlowInfo>(), new HashMap<JetPattern, List<VariableDescriptor>>(), new LabelResolver(), trace, scope, dataFlowInfo, expectedType, expectedReturnType);
    }

    public final JetSemanticServices semanticServices;
    public final BindingTrace trace;
    public final JetScope scope;

    public final DataFlowInfo dataFlowInfo;
    public final JetType expectedType;
    public final JetType expectedReturnType;

    public final Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo;
    public final Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists;
    public final LabelResolver labelResolver;

    private CallResolver callResolver;
    private TypeResolver typeResolver;
    private ClassDescriptorResolver classDescriptorResolver;
    private ExpressionTyperServices services;
    private CompileTimeConstantResolver compileTimeConstantResolver;

    private ExpressionTypingContext(
            @NotNull JetSemanticServices semanticServices,
            @NotNull Map<JetPattern, DataFlowInfo> patternsToDataFlowInfo,
            @NotNull Map<JetPattern, List<VariableDescriptor>> patternsToBoundVariableLists,
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull JetType expectedReturnType) {
        this.trace = trace;
//        this.typeResolver = new TypeResolver(semanticServices, trace, true);
        this.patternsToBoundVariableLists = patternsToBoundVariableLists;
        this.patternsToDataFlowInfo = patternsToDataFlowInfo;
        this.labelResolver = labelResolver;
        this.scope = scope;
//        this.services = new ExpressionTyperServices(semanticServices, trace);
        this.semanticServices = semanticServices;
//        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        this.dataFlowInfo = dataFlowInfo;
        this.expectedType = expectedType;
        this.expectedReturnType = expectedReturnType;
    }

    public ExpressionTypingContext replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, newDataFlowInfo, expectedType, expectedReturnType);
    }

    public ExpressionTypingContext replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, newExpectedType, expectedReturnType);
    }

    public ExpressionTypingContext replaceExpectedReturnType(@Nullable JetType newExpectedReturnType) {
        if (newExpectedReturnType == null) return replaceExpectedReturnType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedReturnType == newExpectedReturnType) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, expectedType, newExpectedReturnType);
    }

    public ExpressionTypingContext replaceBindingTrace(@NotNull BindingTrace newTrace) {
        if (newTrace == trace) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, newTrace, scope, dataFlowInfo, expectedType, expectedReturnType);
    }

    public ExpressionTypingContext replaceExpectedTypeAndTrace(@NotNull JetType newExpectedType, @NotNull BindingTrace newTrace) {
        if (newExpectedType == expectedType && newTrace == trace) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, newTrace, scope, dataFlowInfo, newExpectedType, expectedReturnType);
    }

    @NotNull
    public ExpressionTypingContext replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, newScope, dataFlowInfo, expectedType, expectedReturnType);
    }

    @NotNull
    public ExpressionTypingContext replaceExpectedTypes(@NotNull JetType newExpectedType, @NotNull JetType newExpectedReturnType) {
        if (expectedType == newExpectedType && expectedReturnType == newExpectedReturnType) return this;
        return newContext(semanticServices, patternsToDataFlowInfo, patternsToBoundVariableLists, labelResolver, trace, scope, dataFlowInfo, newExpectedType, newExpectedReturnType);
    }

    public CallResolver getCallResolver() {
        if (callResolver == null) {
            callResolver = new CallResolver(semanticServices, dataFlowInfo);
        }
        return callResolver;
    }

    @Nullable
    public FunctionDescriptor resolveCallWithGivenName(@NotNull Call call, @NotNull JetReferenceExpression functionReference, @NotNull String name, @NotNull ReceiverDescriptor receiver) {
        return getCallResolver().resolveCallWithGivenName(trace, scope, call, functionReference, name, expectedType);
    }

    @Nullable
    public JetType resolveCall(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetCallExpression callExpression) {
        return getCallResolver().resolveCall(trace, scope, CallMaker.makeCall(receiver, callOperationNode, callExpression), expectedType);
    }

    @Nullable
    public VariableDescriptor resolveSimpleProperty(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetSimpleNameExpression nameExpression) {
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        return getCallResolver().resolveSimpleProperty(trace, scope, call, expectedType);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull ReceiverDescriptor receiver, @NotNull String name, @NotNull List<JetType> parameterTypes) {
        return getCallResolver().resolveExactSignature(scope, receiver, name, parameterTypes);
    }

    public ExpressionTyperServices getServices() {
        if (services == null) {
            services = new ExpressionTyperServices(semanticServices, trace);
        }
        return services;
    }

    public TypeResolver getTypeResolver() {
        if (typeResolver == null) {
            typeResolver = new TypeResolver(semanticServices, trace, true);
        }
        return typeResolver;
    }

    public ClassDescriptorResolver getClassDescriptorResolver() {
        if (classDescriptorResolver == null) {
            classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        }
        return classDescriptorResolver;
    }

    public CompileTimeConstantResolver getCompileTimeConstantResolver() {
        if (compileTimeConstantResolver == null) {
            compileTimeConstantResolver = new CompileTimeConstantResolver(semanticServices, trace);
        }
        return compileTimeConstantResolver;
    }
}
