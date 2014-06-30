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
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueConstant;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.expressions.BasicExpressionTypingVisitor;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiversPackage.createQualifier;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiversPackage.resolveAsStandaloneExpression;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    public ResolvedCall<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call, @NotNull JetExpression callExpression,
            @NotNull ResolutionContext context, @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull boolean[] result
    ) {
        CallResolver callResolver = expressionTypingServices.getCallResolver();
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
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

        // if the expression is a receiver in a qualified expression, it should be resolved after the selector is resolved
        boolean isLHSOfDot = JetPsiUtil.isLHSOfDot(nameExpression);
        if (!resolutionResult.isNothing()) {
            boolean isQualifier = isLHSOfDot && resolutionResult.isSingleResult()
                                  && resolutionResult.getResultingDescriptor() instanceof FakeCallableDescriptorForObject;
            if (!isQualifier) {
                result[0] = true;
                temporaryForVariable.commit();
                checkSuper(receiver, resolutionResult, context.trace, nameExpression);
                return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
            }
        }

        QualifierReceiver qualifier = createQualifier(nameExpression, receiver, context);
        if (qualifier != null) {
            result[0] = true;
            if (!isLHSOfDot) {
                resolveAsStandaloneExpression(qualifier, context);
            }
            return null;
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
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
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
            if (functionDescriptor instanceof ConstructorDescriptor && DescriptorUtils.isAnnotationClass(functionDescriptor.getContainingDeclaration())) {
                if (!canInstantiateAnnotationClass(callExpression)) {
                    context.trace.report(ANNOTATION_CLASS_CONSTRUCTOR_CALL.on(callExpression));
                }
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

    private static boolean canInstantiateAnnotationClass(@NotNull JetCallExpression expression) {
        PsiElement parent = expression.getParent();
        if (parent instanceof JetValueArgument) {
            return PsiTreeUtil.getParentOfType(parent, JetAnnotationEntry.class) != null;
        }
        else if (parent instanceof JetParameter) {
            JetClass jetClass = PsiTreeUtil.getParentOfType(parent, JetClass.class);
            if (jetClass != null) {
                return jetClass.hasModifier(JetTokens.ANNOTATION_KEYWORD);
            }
        }
        return false;
    }

    private static void checkSuper(
            @NotNull ReceiverValue receiverValue,
            @NotNull OverloadResolutionResults<?> results,
            @NotNull BindingTrace trace,
            @NotNull JetExpression expression
    ) {
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
            @Nullable JetExpression selectorExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (selectorExpression instanceof JetCallExpression) {
            return getCallExpressionTypeInfoWithoutFinalTypeCheck((JetCallExpression) selectorExpression, receiver,
                                                                  callOperationNode, context);
        }
        else if (selectorExpression instanceof JetSimpleNameExpression) {
            return getSimpleNameExpressionTypeInfo((JetSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression != null) {
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
        QualifierReceiver qualifierReceiver = (QualifierReceiver) context.trace.get(BindingContext.QUALIFIER, receiverExpression);

        if (receiverType == null) receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());

        context = context.replaceDataFlowInfo(receiverTypeInfo.getDataFlowInfo());

        ReceiverValue receiver = qualifierReceiver == null ? new ExpressionReceiver(receiverExpression, receiverType) : qualifierReceiver;
        JetTypeInfo selectorReturnTypeInfo = getSelectorReturnTypeInfo(
                receiver, expression.getOperationTokenNode(), selectorExpression, context);
        JetType selectorReturnType = selectorReturnTypeInfo.getType();

        resolveDeferredReceiverInQualifiedExpression(qualifierReceiver, expression, context);
        checkNestedClassAccess(expression, context);

        //TODO move further
        if (expression.getOperationSign() == JetTokens.SAFE_ACCESS) {
            if (selectorReturnType != null && !KotlinBuiltIns.getInstance().isUnit(selectorReturnType)) {
                if (TypeUtils.isNullableType(receiverType)) {
                    selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                }
            }
        }

        // TODO : this is suspicious: remove this code?
        if (selectorReturnType != null) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, selectorExpression, selectorReturnType);
        }

        CompileTimeConstant<?> value = ConstantExpressionEvaluator.OBJECT$.evaluate(expression, context.trace, context.expectedType);
        if (value instanceof IntegerValueConstant && ((IntegerValueConstant) value).isPure()) {
            return BasicExpressionTypingVisitor.createCompileTimeConstantTypeInfo(value, expression, context);
        }

        JetTypeInfo typeInfo = JetTypeInfo.create(selectorReturnType, selectorReturnTypeInfo.getDataFlowInfo());
        if (context.contextDependency == INDEPENDENT) {
            DataFlowUtils.checkType(typeInfo, expression, context);
        }
        return typeInfo;
    }

    private static void resolveDeferredReceiverInQualifiedExpression(
            @Nullable QualifierReceiver qualifierReceiver,
            @NotNull JetQualifiedExpression qualifiedExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (qualifierReceiver == null) return;
        JetExpression calleeExpression =
                JetPsiUtil.deparenthesize(CallUtilPackage.getCalleeExpressionIfAny(qualifiedExpression.getSelectorExpression()), false);
        DeclarationDescriptor selectorDescriptor =
                calleeExpression instanceof JetReferenceExpression
                ? context.trace.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) calleeExpression) : null;
        ReceiversPackage.resolveAsReceiverInQualifiedExpression(qualifierReceiver, context, selectorDescriptor);
    }

    private static void checkNestedClassAccess(
            @NotNull JetQualifiedExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        JetExpression selectorExpression = expression.getSelectorExpression();
        if (selectorExpression == null) return;

        // A.B - if B is a nested class accessed by outer class, 'A' and 'A.B' were marked as qualifiers
        // a.B - if B is a nested class accessed by instance reference, 'a.B' was marked as a qualifier, but 'a' was not (it's an expression)

        Qualifier expressionQualifier = context.trace.get(BindingContext.QUALIFIER, expression);
        Qualifier receiverQualifier = context.trace.get(BindingContext.QUALIFIER, expression.getReceiverExpression());

        if (receiverQualifier == null && expressionQualifier != null) {
            assert expressionQualifier.getClassifier() instanceof ClassDescriptor :
                    "Only class can (package cannot) be accessed by instance reference: " + expressionQualifier;
            context.trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.on(selectorExpression, (ClassDescriptor)expressionQualifier.getClassifier()));
        }
    }
}
