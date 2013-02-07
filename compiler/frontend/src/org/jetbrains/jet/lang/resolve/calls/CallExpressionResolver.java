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

import com.google.common.base.Predicate;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.FilteringScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;

import java.util.Collections;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLUTION_SCOPE;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {
    @Nullable
    private static JetType lookupNamespaceOrClassObject(@NotNull JetSimpleNameExpression expression, @NotNull ExpressionTypingContext context) {
        Name referencedName = expression.getReferencedNameAsName();
        ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
        if (classifier != null) {
            JetType classObjectType = classifier.getClassObjectType();
            if (classObjectType != null) {
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                JetType result;
                if (context.namespacesAllowed && classifier instanceof ClassDescriptor) {
                    JetScope scope = new ChainedScope(classifier, classObjectType.getMemberScope(),
                                                      getStaticNestedClassesScope((ClassDescriptor) classifier));
                    result = new NamespaceType(referencedName, scope);
                }
                else if (context.namespacesAllowed || classifier.isClassObjectAValue()) {
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
            if (!context.namespacesAllowed) {
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

    @NotNull
    private static JetScope getStaticNestedClassesScope(@NotNull ClassDescriptor descriptor) {
        JetScope innerClassesScope = descriptor.getUnsubstitutedInnerClassesScope();
        return new FilteringScope(innerClassesScope, new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@Nullable DeclarationDescriptor descriptor) {
                return descriptor instanceof ClassDescriptor && !((ClassDescriptor) descriptor).isInner();
            }
        });
    }

    private static boolean furtherNameLookup(
            @NotNull JetSimpleNameExpression expression,
            @NotNull JetType[] result,
            @NotNull ExpressionTypingContext context
    ) {
        NamespaceType namespaceType = lookupNamespaceType(expression, context);
        if (namespaceType == null) {
            return false;
        }
        if (context.namespacesAllowed) {
            result[0] = namespaceType;
            return true;
        }
        context.trace.report(EXPRESSION_EXPECTED_NAMESPACE_FOUND.on(expression));
        result[0] = ErrorUtils.createErrorType("Type for " + expression.getReferencedNameAsName());
        return false;
    }

    @Nullable
    private static NamespaceType lookupNamespaceType(@NotNull JetSimpleNameExpression expression, @NotNull ExpressionTypingContext context) {
        Name name = expression.getReferencedNameAsName();
        NamespaceDescriptor namespace = context.scope.getNamespace(name);
        if (namespace == null) {
            return null;
        }
        context.trace.record(REFERENCE_TARGET, expression, namespace);

        // Construct a NamespaceType with everything from the namespace and with static nested classes of the corresponding class (if any)
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
    private static ResolvedCall<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call, @NotNull JetExpression callExpression, @NotNull ReceiverValue receiver,
            @NotNull ExpressionTypingContext context, @NotNull boolean[] result
    ) {

        OverloadResolutionResults<FunctionDescriptor> results = context.resolveFunctionCall(call);
        if (!results.isNothing()) {
            checkSuper(receiver, results, context.trace, callExpression);
            result[0] = true;
            if (results.isIncomplete()) {
                return null;
            }
            return results.isSingleResult() ? results.getResultingCall() : null;
        }
        result[0] = false;
        return null;
    }

    @Nullable
    private static JetType getVariableType(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context, @NotNull boolean[] result) {

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(
                context.trace, "trace to resolve as local variable or property", nameExpression);
        OverloadResolutionResults<VariableDescriptor> resolutionResult = context.replaceBindingTrace(traceForVariable).resolveSimpleProperty(receiver, callOperationNode, nameExpression);
        if (!resolutionResult.isNothing()) {
            traceForVariable.commit();
            checkSuper(receiver, resolutionResult, context.trace, nameExpression);
            result[0] = true;
            return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
        }

        ExpressionTypingContext newContext = receiver.exists()
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
    public static JetTypeInfo getSimpleNameExpressionTypeInfo(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context) {

        boolean[] result = new boolean[1];

        TemporaryBindingTrace traceForVariable = TemporaryBindingTrace.create(context.trace, "trace to resolve as variable", nameExpression);
        JetType type = getVariableType(nameExpression, receiver, callOperationNode, context.replaceBindingTrace(traceForVariable), result);
        if (result[0]) {
            traceForVariable.commit();
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace, "trace to resolve as function", nameExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(call, nameExpression, receiver, context, result);
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
    public static JetTypeInfo getCallExpressionTypeInfo(@NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context) {

        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryBindingTrace traceForFunction = TemporaryBindingTrace.create(context.trace, "trace to resolve as function call", callExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(call, callExpression, receiver,
                                                                                   context.replaceBindingTrace(traceForFunction), result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            traceForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty()) {
                // there are only type arguments
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
            }
            if (functionDescriptor == null) {
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
            JetType type = functionDescriptor.getReturnType();

            return JetTypeInfo.create(type, resolvedCall.getDataFlowInfo());
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
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
        }
        traceForFunction.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    private static void checkSuper(@NotNull ReceiverValue receiverValue, @NotNull OverloadResolutionResults<? extends CallableDescriptor> results,
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
    public static JetTypeInfo getSelectorReturnTypeInfo(
            @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode,
            @NotNull JetExpression selectorExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (selectorExpression instanceof JetCallExpression) {
            return getCallExpressionTypeInfo((JetCallExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            return getSimpleNameExpressionTypeInfo((JetSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) selectorExpression;
            JetExpression newReceiverExpression = qualifiedExpression.getReceiverExpression();
            JetTypeInfo newReceiverTypeInfo = getSelectorReturnTypeInfo(receiver, callOperationNode, newReceiverExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
            JetType newReceiverType = newReceiverTypeInfo.getType();
            DataFlowInfo newReceiverDataFlowInfo = newReceiverTypeInfo.getDataFlowInfo();
            JetExpression newSelectorExpression = qualifiedExpression.getSelectorExpression();
            if (newReceiverType != null && newSelectorExpression != null) {
                return getSelectorReturnTypeInfo(new ExpressionReceiver(newReceiverExpression, newReceiverType), qualifiedExpression.getOperationTokenNode(), newSelectorExpression, context.replaceDataFlowInfo(newReceiverDataFlowInfo));
            }
        }
        else {
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }
}
