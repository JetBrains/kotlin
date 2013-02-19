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

package org.jetbrains.jet.lang.resolve.calls;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.ConstantUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getStaticNestedClassesScope;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    private JetType lookupNamespaceOrClassObject(@NotNull JetSimpleNameExpression expression, @NotNull ResolutionContext context) {
        Name referencedName = expression.getReferencedNameAsName();
        ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
        if (classifier != null) {
            JetType classObjectType = classifier.getClassObjectType();
            if (classObjectType != null) {
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                JetType result;
                if (context.expressionPosition == ExpressionPosition.LHS_OF_DOT && classifier instanceof ClassDescriptor) {
                    JetScope scope = new ChainedScope(classifier, classObjectType.getMemberScope(),
                                                      getStaticNestedClassesScope((ClassDescriptor) classifier));
                    result = new NamespaceType(referencedName, scope);
                }
                else if (context.expressionPosition == ExpressionPosition.LHS_OF_DOT || classifier.isClassObjectAValue()) {
                    result = classObjectType;
                }
                else {
                    context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
                    result = null;
                }
                return DataFlowUtils.checkType(result, expression, context);
            }
        }
        JetType[] result = new JetType[1];
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                context.trace, "trace for namespace/class object lookup of name", referencedName);
        if (furtherNameLookup(expression, result, context.replaceBindingTrace(temporaryTrace))) {
            temporaryTrace.commit();
            return DataFlowUtils.checkType(result[0], expression, context);
        }
        // To report NO_CLASS_OBJECT when no namespace found
        if (classifier != null) {
            if (context.expressionPosition == ExpressionPosition.FREE) {
                context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
            }
            context.trace.record(REFERENCE_TARGET, expression, classifier);
            JetScope scopeForStaticMembersResolution = classifier instanceof ClassDescriptor
                                                       ? getStaticNestedClassesScope((ClassDescriptor) classifier)
                                                       : JetScope.EMPTY;
            return new NamespaceType(referencedName, scopeForStaticMembersResolution);
        }
        temporaryTrace.commit();
        return result[0];
    }

    private boolean furtherNameLookup(
            @NotNull JetSimpleNameExpression expression,
            @NotNull JetType[] result,
            @NotNull ResolutionContext context
    ) {
        NamespaceType namespaceType = lookupNamespaceType(expression, context);
        if (namespaceType == null) {
            return false;
        }
        if (context.expressionPosition == ExpressionPosition.LHS_OF_DOT) {
            result[0] = namespaceType;
            return true;
        }
        context.trace.report(EXPRESSION_EXPECTED_NAMESPACE_FOUND.on(expression));
        result[0] = ErrorUtils.createErrorType("Type for " + expression.getReferencedNameAsName());
        return false;
    }

    @Nullable
    private NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull ResolutionContext context) {
        Name name = expression.getReferencedNameAsName();
        NamespaceDescriptor namespace = context.scope.getNamespace(name);
        if (namespace == null) {
            return null;
        }
        context.trace.record(REFERENCE_TARGET, expression, namespace);

        // Construct a NamespaceType with everything from the namespace and with nested classes of the corresponding class (if any)
        JetScope scope;
        ClassifierDescriptor classifier = context.scope.getClassifier(name);
        if (classifier instanceof ClassDescriptor) {
            scope = new ChainedScope(namespace, namespace.getMemberScope(), getStaticNestedClassesScope((ClassDescriptor) classifier));
        }
        else {
            scope = namespace.getMemberScope();
        }
        return new NamespaceType(name, scope);
    }

    @Nullable
    private ResolvedCall<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call, @NotNull JetExpression callExpression, @NotNull ReceiverValue receiver,
            @NotNull ResolutionContext context, @NotNull ResolveMode resolveMode, @NotNull boolean[] result
    ) {

        CallResolver callResolver = expressionTypingServices.getCallResolver();
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                BasicCallResolutionContext.create(context, call, resolveMode));
        if (!results.isNothing()) {
            checkSuper(receiver, results, context.trace, callExpression);
            result[0] = true;
            if (results.isSingleResult() && resolveMode == ResolveMode.TOP_LEVEL_CALL) {
                if (CallResolverUtil.hasReturnTypeDependentOnNotInferredParams(results.getResultingCall())) {
                    return null;
                }
            }
            return results.isSingleResult() ? results.getResultingCall() : null;
        }
        result[0] = false;
        return null;
    }

    @Nullable
    private JetType getVariableType(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ResolutionContext context, @NotNull boolean[] result) {

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(
                context.trace, "trace to resolve as local variable or property", nameExpression);
        CallResolver callResolver = expressionTypingServices.getCallResolver();
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        OverloadResolutionResults<VariableDescriptor> resolutionResult = callResolver.resolveSimpleProperty(
                BasicCallResolutionContext.create(context.replaceBindingTrace(traceForVariable), call, ResolveMode.TOP_LEVEL_CALL));
        if (!resolutionResult.isNothing()) {
            traceForVariable.commit();
            checkSuper(receiver, resolutionResult, context.trace, nameExpression);
            result[0] = true;
            return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
        }

        ResolutionContext newContext = receiver.exists()
                                             ? context.replaceScope(receiver.getType().getMemberScope())
                                             : context;
        TemporaryBindingTrace traceForNamespaceOrClassObject = TemporaryBindingTrace.create(
                context.trace, "trace to resolve as namespace or class object", nameExpression);
        JetType jetType = lookupNamespaceOrClassObject(nameExpression, newContext.replaceBindingTrace(traceForNamespaceOrClassObject));
        if (jetType != null) {
            traceForNamespaceOrClassObject.commit();

            // Uncommitted changes in temp context
            context.trace.record(RESOLUTION_SCOPE, nameExpression, context.scope);
            if (context.dataFlowInfo.hasTypeInfoConstraints()) {
                context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, nameExpression, context.dataFlowInfo);
            }
            result[0] = true;
            return jetType;
        }
        result[0] = false;
        return null;
    }

    @NotNull
    public JetTypeInfo getSimpleNameExpressionTypeInfo(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ResolutionContext context) {

        boolean[] result = new boolean[1];

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace, "trace to resolve as variable", nameExpression);
        JetType type = getVariableType(nameExpression, receiver, callOperationNode, context.replaceBindingTrace(traceForVariable), result);
        if (result[0]) {
            traceForVariable.commit();
            if (type instanceof NamespaceType && context.expressionPosition == ExpressionPosition.FREE) {
                type = null;
            }
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace, "trace to resolve as function", nameExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(call, nameExpression, receiver, context, ResolveMode.TOP_LEVEL_CALL, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            traceForFunction.commit();
            boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
            context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
            type = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        traceForVariable.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo getCallExpressionTypeInfo(@NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ResolutionContext context) {
        return getCallExpressionTypeInfoForCall(callExpression, receiver, callOperationNode, context, ResolveMode.TOP_LEVEL_CALL).getTypeInfo();
    }

    @NotNull
    public TypeInfoForCall getCallExpressionTypeInfoForCall(
            @NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ResolutionContext context, @NotNull ResolveMode resolveMode
    ) {
        TypeInfoForCall typeInfoForCall = getCallExpressionTypeInfoForCallWithoutFinalTypeCheck(callExpression, receiver, callOperationNode,
                                                                                                context, resolveMode);
        if (resolveMode == ResolveMode.TOP_LEVEL_CALL) {
            DataFlowUtils.checkType(typeInfoForCall.getType(), callExpression, context, typeInfoForCall.getDataFlowInfo());
        }
        return typeInfoForCall;
    }

    @NotNull
    public TypeInfoForCall getCallExpressionTypeInfoForCallWithoutFinalTypeCheck(
            @NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ResolutionContext context, @NotNull ResolveMode resolveMode
    ) {
        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace, "trace to resolve as function call", callExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, callExpression, receiver, context.replaceBindingTrace(traceForFunction), resolveMode, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            traceForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty()) {
                // there are only type arguments
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
            }
            if (functionDescriptor == null) {
                return TypeInfoForCall.create(null, context.dataFlowInfo);
            }
            JetType type = functionDescriptor.getReturnType();

            return TypeInfoForCall.create(type, resolvedCall.getDataFlowInfo(), resolvedCall, call, context, resolveMode);
        }

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression && callExpression.getTypeArgumentList() == null) {
            TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(
                    context.trace, "trace to resolve as variable with 'invoke' call", callExpression);
            JetType type = getVariableType((JetSimpleNameExpression) calleeExpression, receiver, callOperationNode,
                                           context.replaceBindingTrace(traceForVariable), result);
            if (result[0]) {
                traceForVariable.commit();
                context.trace.report(FUNCTION_EXPECTED.on((JetReferenceExpression) calleeExpression, calleeExpression,
                                                          type != null ? type : ErrorUtils.createErrorType("")));
                return TypeInfoForCall.create(null, context.dataFlowInfo);
            }
        }
        traceForFunction.commit();
        return TypeInfoForCall.create(null, context.dataFlowInfo);
    }

    private void checkSuper(@NotNull ReceiverValue receiverValue, @NotNull OverloadResolutionResults<? extends CallableDescriptor> results,
            @NotNull BindingTrace trace, @NotNull JetExpression expression) {
        if (!results.isSingleResult()) return;
        if (!(receiverValue instanceof ExpressionReceiver)) return;
        JetExpression receiver = ((ExpressionReceiver) receiverValue).getExpression();
        CallableDescriptor descriptor = results.getResultingDescriptor();
        if (receiver instanceof JetSuperExpression && descriptor instanceof MemberDescriptor) {
            if (((MemberDescriptor) descriptor).getModality() == Modality.ABSTRACT) {
                trace.report(ABSTRACT_SUPER_CALL.on(expression));
            }
        }
    }

    @NotNull
    private TypeInfoForCall getSelectorReturnTypeInfo(
            @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode,
            @NotNull JetExpression selectorExpression,
            @NotNull ResolutionContext context,
            @NotNull ResolveMode resolveMode
    ) {
        if (selectorExpression instanceof JetCallExpression) {
            return getCallExpressionTypeInfoForCallWithoutFinalTypeCheck((JetCallExpression) selectorExpression, receiver,
                                                                         callOperationNode, context, resolveMode);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            return TypeInfoForCall.create(
                    getSimpleNameExpressionTypeInfo((JetSimpleNameExpression) selectorExpression, receiver, callOperationNode, context));
        }
        else if (selectorExpression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) selectorExpression;
            JetExpression newReceiverExpression = qualifiedExpression.getReceiverExpression();
            TypeInfoForCall newReceiverTypeInfo = getSelectorReturnTypeInfo(
                    receiver, callOperationNode, newReceiverExpression, context.replaceExpectedType(NO_EXPECTED_TYPE), resolveMode);
            JetType newReceiverType = newReceiverTypeInfo.getType();
            DataFlowInfo newReceiverDataFlowInfo = newReceiverTypeInfo.getDataFlowInfo();
            JetExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
            if (newReceiverType != null && newSelectorExpression != null) {
                ExpressionReceiver expressionReceiver = new ExpressionReceiver(newReceiverExpression, newReceiverType);
                return getSelectorReturnTypeInfo(
                        expressionReceiver, qualifiedExpression.getOperationTokenNode(),
                        newSelectorExpression, context.replaceDataFlowInfo(newReceiverDataFlowInfo), resolveMode);
            }
        }
        else {
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return TypeInfoForCall.create(null, context.dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo getQualifiedExpressionTypeInfo(@NotNull JetQualifiedExpression expression, @NotNull ResolutionContext context) {
        return getQualifiedExpressionExtendedTypeInfo(expression, context, ResolveMode.TOP_LEVEL_CALL).getTypeInfo();
    }

    @NotNull
    public TypeInfoForCall getQualifiedExpressionExtendedTypeInfo(
            @NotNull JetQualifiedExpression expression, @NotNull ResolutionContext context, @NotNull ResolveMode resolveMode
    ) {
        // TODO : functions as values
        JetExpression selectorExpression = expression.getSelectorExpression();
        JetExpression receiverExpression = expression.getReceiverExpression();
        JetTypeInfo receiverTypeInfo = expressionTypingServices.getTypeInfoWithNamespaces(
                receiverExpression, context.scope, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
        JetType receiverType = receiverTypeInfo.getType();
        if (selectorExpression == null) return TypeInfoForCall.create(null, context.dataFlowInfo);
        if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

        context = context.replaceDataFlowInfo(receiverTypeInfo.getDataFlowInfo());

        if (selectorExpression instanceof JetSimpleNameExpression) {
            ConstantUtils.propagateConstantValues(expression, context.trace, (JetSimpleNameExpression) selectorExpression);
        }

        TypeInfoForCall selectorReturnTypeInfo = getSelectorReturnTypeInfo(
                new ExpressionReceiver(receiverExpression, receiverType), expression.getOperationTokenNode(), selectorExpression, context, resolveMode);
        JetType selectorReturnType = selectorReturnTypeInfo.getType();

        //TODO move further
        if (!(receiverType instanceof NamespaceType) && expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
            if (selectorReturnType != null && !selectorReturnType.isNullable() && !KotlinBuiltIns.getInstance().isUnit(selectorReturnType)) {
                if (receiverType.isNullable()) {
                    selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                }
            }
        }

        // TODO : this is suspicious: remove this code?
        if (selectorReturnType != null) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, selectorReturnType);
        }
        JetTypeInfo typeInfo = JetTypeInfo.create(selectorReturnType, selectorReturnTypeInfo.getDataFlowInfo());
        if (resolveMode == ResolveMode.TOP_LEVEL_CALL) {
            DataFlowUtils.checkType(typeInfo.getType(), expression, context, typeInfo.getDataFlowInfo());
        }
        return TypeInfoForCall.create(typeInfo, selectorReturnTypeInfo);
    }
}
