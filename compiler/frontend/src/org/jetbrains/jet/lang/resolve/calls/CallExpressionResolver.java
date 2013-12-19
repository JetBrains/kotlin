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
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.expressions.BasicExpressionTypingVisitor;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.utils.Printer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.psi.JetPsiUtil.isLHSOfDot;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getStaticNestedClassesScope;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    private JetType lookupNamespaceOrClassObject(@NotNull JetSimpleNameExpression expression, @NotNull ExpressionTypingContext context) {
        Name referencedName = expression.getReferencedNameAsName();
        final ClassifierDescriptor classifier = context.scope.getClassifier(referencedName);
        if (classifier != null) {
            JetType classObjectType = classifier.getClassObjectType();
            if (classObjectType != null) {
                context.trace.record(REFERENCE_TARGET, expression, classifier);
                JetType result = getExtendedClassObjectType(expression, classObjectType, classifier, context);
                checkClassObjectVisibility(classifier, expression, context);
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
            if (classifier instanceof TypeParameterDescriptor) {
                if (isLHSOfDot(expression)) {
                    context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(expression, (TypeParameterDescriptor) classifier));
                }
                else {
                    context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(expression, (TypeParameterDescriptor) classifier));
                }
            }
            else if (!isLHSOfDot(expression)) {
                context.trace.report(NO_CLASS_OBJECT.on(expression, classifier));
            }
            context.trace.record(REFERENCE_TARGET, expression, classifier);
            JetScope scopeForStaticMembersResolution =
                    classifier instanceof ClassDescriptor
                    ? getStaticNestedClassesScope((ClassDescriptor) classifier)
                    : new JetScopeImpl() {
                            @NotNull
                            @Override
                            public DeclarationDescriptor getContainingDeclaration() {
                                return classifier;
                            }

                            @Override
                            public String toString() {
                                return "Scope for the type parameter on the left hand side of dot";
                            }

                            @Override
                            public void printScopeStructure(@NotNull Printer p) {
                                p.println(toString(), " for ", classifier);
                            }
                    };
            return new NamespaceType(referencedName, scopeForStaticMembersResolution, NO_RECEIVER);
        }
        temporaryTrace.commit();
        return result[0];
    }

    private static void checkClassObjectVisibility(
            @NotNull ClassifierDescriptor classifier,
            @NotNull JetSimpleNameExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        if (!(classifier instanceof ClassDescriptor)) return;
        ClassDescriptor classObject = ((ClassDescriptor) classifier).getClassObjectDescriptor();
        assert classObject != null : "This check should be done only for classes with class objects: " + classifier;
        DeclarationDescriptor from = context.scopeForVisibility.getContainingDeclaration();
        if (!Visibilities.isVisible(classObject, from)) {
            context.trace.report(INVISIBLE_MEMBER.on(expression, classObject, classObject.getVisibility(), from));
        }
    }

    @NotNull
    private static JetType getExtendedClassObjectType(
            @NotNull JetSimpleNameExpression expression,
            @NotNull JetType classObjectType,
            @NotNull ClassifierDescriptor classifier,
            @NotNull ResolutionContext context
    ) {
        if (!isLHSOfDot(expression) || !(classifier instanceof ClassDescriptor)) {
            return classObjectType;
        }
        ClassDescriptor classDescriptor = (ClassDescriptor) classifier;

        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY) {
            return classObjectType;
        }

        List<JetScope> scopes = new ArrayList<JetScope>(3);

        scopes.add(classObjectType.getMemberScope());
        scopes.add(getStaticNestedClassesScope(classDescriptor));

        Name referencedName = expression.getReferencedNameAsName();
        PackageViewDescriptor namespace = context.scope.getPackage(referencedName);
        if (namespace != null) {
            //for enums loaded from java binaries
            scopes.add(namespace.getMemberScope());
        }

        JetScope scope = new ChainedScope(classifier, scopes.toArray(new JetScope[scopes.size()]));
        return new NamespaceType(referencedName, scope, new ExpressionReceiver(expression, classObjectType));
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
        if (isLHSOfDot(expression)) {
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
        PackageViewDescriptor namespace = context.scope.getPackage(name);
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
        return new NamespaceType(name, scope, NO_RECEIVER);
    }

    @Nullable
    public ResolvedCallWithTrace<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call, @NotNull JetExpression callExpression,
            @NotNull ResolutionContext context, @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull boolean[] result
    ) {
        CallResolver callResolver = expressionTypingServices.getCallResolver();
        OverloadResolutionResultsImpl<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                BasicCallResolutionContext.create(context, call, checkArguments));
        if (!results.isNothing()) {
            checkSuper(call.getExplicitReceiver(), results, context.trace, callExpression);
            result[0] = true;
            return OverloadResolutionResultsUtil.getResultingCall(results, context.contextDependency);
        }
        result[0] = false;
        return null;
    }

    @Nullable
    private JetType getVariableType(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context, @NotNull boolean[] result
    ) {
        TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolve as local variable or property", nameExpression);
        CallResolver callResolver = expressionTypingServices.getCallResolver();
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        BasicCallResolutionContext contextForVariable = BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForVariable),
                call, CheckValueArgumentsMode.ENABLED);
        OverloadResolutionResults<VariableDescriptor> resolutionResult = callResolver.resolveSimpleProperty(contextForVariable);
        if (resolutionResult.isSuccess()) {
            temporaryForVariable.commit();
            checkSuper(receiver, resolutionResult, context.trace, nameExpression);
            result[0] = true;
            return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
        }

        ExpressionTypingContext newContext = receiver.exists()
                                             ? context.replaceScope(receiver.getType().getMemberScope())
                                             : context;
        TemporaryTraceAndCache temporaryForNamespaceOrClassObject = TemporaryTraceAndCache.create(
                context, "trace to resolve as namespace or class object", nameExpression);
        JetType jetType = lookupNamespaceOrClassObject(nameExpression, newContext.replaceTraceAndCache(temporaryForNamespaceOrClassObject));
        if (jetType != null) {
            temporaryForNamespaceOrClassObject.commit();

            // Uncommitted changes in temp context
            context.trace.record(RESOLUTION_SCOPE, nameExpression, context.scope);
            if (context.dataFlowInfo.hasTypeInfoConstraints()) {
                context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, nameExpression, context.dataFlowInfo);
            }
            result[0] = true;
            return jetType;
        }
        temporaryForVariable.commit();
        result[0] = !resolutionResult.isNothing();
        return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
    }

    @NotNull
    public JetTypeInfo getSimpleNameExpressionTypeInfo(@NotNull JetSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];

        TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolve as variable", nameExpression);
        JetType type = getVariableType(nameExpression, receiver, callOperationNode, context.replaceTraceAndCache(temporaryForVariable), result);
        if (result[0]) {
            temporaryForVariable.commit();
            if (type instanceof NamespaceType && !isLHSOfDot(nameExpression)) {
                type = null;
            }
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function", nameExpression);
        ResolutionContext newContext = context.replaceTraceAndCache(temporaryForFunction);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, nameExpression, newContext, CheckValueArgumentsMode.ENABLED, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            temporaryForFunction.commit();
            boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
            context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
            type = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }

        temporaryForVariable.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo getCallExpressionTypeInfo(
            @NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        JetTypeInfo typeInfo = getCallExpressionTypeInfoWithoutFinalTypeCheck(callExpression, receiver, callOperationNode, context);
        if (context.contextDependency == INDEPENDENT) {
            DataFlowUtils.checkType(typeInfo, callExpression, context);
        }
        return typeInfo;
    }

    @NotNull
    public JetTypeInfo getCallExpressionTypeInfoWithoutFinalTypeCheck(
            @NotNull JetCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function call", callExpression);
        ResolvedCallWithTrace<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, callExpression, context.replaceTraceAndCache(temporaryForFunction),
                CheckValueArgumentsMode.ENABLED, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            temporaryForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty()) {
                // there are only type arguments
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
            }
            if (functionDescriptor == null) {
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
            JetType type = functionDescriptor.getReturnType();

            return JetTypeInfo.create(type, resolvedCall.getDataFlowInfoForArguments().getResultInfo());
        }

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression && callExpression.getTypeArgumentList() == null) {
            TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                    context, "trace to resolve as variable with 'invoke' call", callExpression);
            JetType type = getVariableType((JetSimpleNameExpression) calleeExpression, receiver, callOperationNode,
                                           context.replaceTraceAndCache(temporaryForVariable), result);
            if (result[0]) {
                temporaryForVariable.commit();
                context.trace.report(FUNCTION_EXPECTED.on((JetReferenceExpression) calleeExpression, calleeExpression,
                                                          type != null ? type : ErrorUtils.createErrorType("")));
                return JetTypeInfo.create(null, context.dataFlowInfo);
            }
        }
        temporaryForFunction.commit();
        return JetTypeInfo.create(null, context.dataFlowInfo);
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
    private JetTypeInfo getSelectorReturnTypeInfo(
            @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode,
            @NotNull JetExpression selectorExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (selectorExpression instanceof JetCallExpression) {
            return getCallExpressionTypeInfoWithoutFinalTypeCheck((JetCallExpression) selectorExpression, receiver,
                                                                  callOperationNode, context);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            return getSimpleNameExpressionTypeInfo((JetSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else {
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return JetTypeInfo.create(null, context.dataFlowInfo);
    }

    @NotNull
    public JetTypeInfo getQualifiedExpressionTypeInfo(
            @NotNull JetQualifiedExpression expression, @NotNull ExpressionTypingContext context
    ) {
        // TODO : functions as values
        JetExpression selectorExpression = expression.getSelectorExpression();
        JetExpression receiverExpression = expression.getReceiverExpression();
        ResolutionContext contextForReceiver = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetTypeInfo receiverTypeInfo = expressionTypingServices.getTypeInfo(receiverExpression, contextForReceiver);
        JetType receiverType = receiverTypeInfo.getType();
        if (selectorExpression == null) return JetTypeInfo.create(null, context.dataFlowInfo);
        if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

        context = context.replaceDataFlowInfo(receiverTypeInfo.getDataFlowInfo());

        JetTypeInfo selectorReturnTypeInfo = getSelectorReturnTypeInfo(
                new ExpressionReceiver(receiverExpression, receiverType),
                expression.getOperationTokenNode(), selectorExpression, context);
        JetType selectorReturnType = selectorReturnTypeInfo.getType();

        //TODO move further
        if (!(receiverType instanceof NamespaceType) && expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
            if (selectorReturnType != null && !KotlinBuiltIns.getInstance().isUnit(selectorReturnType)) {
                if (receiverType.isNullable()) {
                    selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                }
            }
        }

        // TODO : this is suspicious: remove this code?
        if (selectorReturnType != null) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, selectorReturnType);
        }

        CompileTimeConstant<?> value = ConstantExpressionEvaluator.object$.evaluate(expression, context.trace, context.expectedType);
        if (value != null) {
           return BasicExpressionTypingVisitor.createCompileTimeConstantTypeInfo(value, expression, context);
        }

        JetTypeInfo typeInfo = JetTypeInfo.create(selectorReturnType, selectorReturnTypeInfo.getDataFlowInfo());
        if (context.contextDependency == INDEPENDENT) {
            DataFlowUtils.checkType(typeInfo, expression, context);
        }
        return typeInfo;
    }
}
