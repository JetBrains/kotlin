/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.receivers.*;
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {

    @NotNull private final CallResolver callResolver;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;
    @NotNull private final DataFlowAnalyzer dataFlowAnalyzer;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final QualifiedExpressionResolver qualifiedExpressionResolver;
    @NotNull private final SymbolUsageValidator symbolUsageValidator;

    public CallExpressionResolver(
            @NotNull CallResolver callResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull DataFlowAnalyzer dataFlowAnalyzer,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull QualifiedExpressionResolver qualifiedExpressionResolver,
            @NotNull SymbolUsageValidator symbolUsageValidator
    ) {
        this.callResolver = callResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.dataFlowAnalyzer = dataFlowAnalyzer;
        this.builtIns = builtIns;
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
        this.symbolUsageValidator = symbolUsageValidator;
    }

    private ExpressionTypingServices expressionTypingServices;

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    public ResolvedCall<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call,
            @NotNull ResolutionContext context, @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull boolean[] result
    ) {
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                BasicCallResolutionContext.create(context, call, checkArguments));
        if (!results.isNothing()) {
            result[0] = true;
            return OverloadResolutionResultsUtil.getResultingCall(results, context.contextDependency);
        }
        result[0] = false;
        return null;
    }

    @Nullable
    private KotlinType getVariableType(
            @NotNull KtSimpleNameExpression nameExpression, @Nullable Receiver receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context, @NotNull boolean[] result
    ) {
        TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolve as local variable or property", nameExpression);
        Call call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression);
        BasicCallResolutionContext contextForVariable = BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForVariable),
                call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        OverloadResolutionResults<VariableDescriptor> resolutionResult = callResolver.resolveSimpleProperty(contextForVariable);

        // if the expression is a receiver in a qualified expression, it should be resolved after the selector is resolved
        boolean isLHSOfDot = KtPsiUtil.isLHSOfDot(nameExpression);
        if (!resolutionResult.isNothing() && resolutionResult.getResultCode() != OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER) {
            boolean isQualifier = isLHSOfDot &&
                                  resolutionResult.isSingleResult() &&
                                  resolutionResult.getResultingDescriptor() instanceof FakeCallableDescriptorForObject;
            if (!isQualifier) {
                result[0] = true;
                temporaryForVariable.commit();
                return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
            }
        }

        temporaryForVariable.commit();
        result[0] = !resolutionResult.isNothing();
        return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
    }

    @NotNull
    public KotlinTypeInfo getSimpleNameExpressionTypeInfo(
            @NotNull KtSimpleNameExpression nameExpression, @Nullable Receiver receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];

        TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolve as variable", nameExpression);
        KotlinType type =
                getVariableType(nameExpression, receiver, callOperationNode, context.replaceTraceAndCache(temporaryForVariable), result);

        if (result[0]) {
            temporaryForVariable.commit();
            return TypeInfoFactoryKt.createTypeInfo(type, context);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function", nameExpression);
        ResolutionContext newContext = context.replaceTraceAndCache(temporaryForFunction);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, newContext, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            if (!(functionDescriptor instanceof ConstructorDescriptor)) {
                temporaryForFunction.commit();
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
                type = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
                return TypeInfoFactoryKt.createTypeInfo(type, context);
            }
        }

        TemporaryTraceAndCache temporaryForQualifier = TemporaryTraceAndCache.create(context, "trace to resolve as qualifier", nameExpression);
        ExpressionTypingContext contextForQualifier = context.replaceTraceAndCache(temporaryForQualifier);
        Qualifier qualifier = qualifiedExpressionResolver.resolveNameExpressionAsQualifierForDiagnostics(nameExpression, receiver, contextForQualifier);
        if (qualifier != null) {
            QualifiedExpressionResolveUtilKt.resolveQualifierAsStandaloneExpression(qualifier, contextForQualifier, symbolUsageValidator);
            temporaryForQualifier.commit();
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        temporaryForVariable.commit();
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @NotNull
    public KotlinTypeInfo getCallExpressionTypeInfo(
            @NotNull KtCallExpression callExpression, @Nullable ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        KotlinTypeInfo typeInfo = getCallExpressionTypeInfoWithoutFinalTypeCheck(callExpression, receiver, callOperationNode, context);
        if (context.contextDependency == INDEPENDENT) {
            dataFlowAnalyzer.checkType(typeInfo.getType(), callExpression, context);
        }
        return typeInfo;
    }

    /**
     * Visits a call expression and its arguments.
     * Determines the result type and data flow information after the call.
     */
    @NotNull
    private KotlinTypeInfo getCallExpressionTypeInfoWithoutFinalTypeCheck(
            @NotNull KtCallExpression callExpression, @Nullable Receiver receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function call", callExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call,
                context.replaceTraceAndCache(temporaryForFunction),
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            temporaryForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getLambdaArguments().isEmpty()) {
                // there are only type arguments
                boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters));
            }
            if (functionDescriptor == null) {
                return TypeInfoFactoryKt.noTypeInfo(context);
            }
            if (functionDescriptor instanceof ConstructorDescriptor) {
                DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
                if (DescriptorUtils.isAnnotationClass(containingDescriptor)
                    && !canInstantiateAnnotationClass(callExpression, context.trace)) {
                    context.trace.report(ANNOTATION_CLASS_CONSTRUCTOR_CALL.on(callExpression));
                }
                if (DescriptorUtils.isEnumClass(containingDescriptor)) {
                    context.trace.report(ENUM_CLASS_CONSTRUCTOR_CALL.on(callExpression));
                }
                if (containingDescriptor instanceof ClassDescriptor
                    && ((ClassDescriptor) containingDescriptor).getModality() == Modality.SEALED) {
                    context.trace.report(SEALED_CLASS_CONSTRUCTOR_CALL.on(callExpression));
                }
            }

            KotlinType type = functionDescriptor.getReturnType();
            // Extracting jump out possible and jump point flow info from arguments, if any
            List<? extends ValueArgument> arguments = callExpression.getValueArguments();
            DataFlowInfo resultFlowInfo = resolvedCall.getDataFlowInfoForArguments().getResultInfo();
            DataFlowInfo jumpFlowInfo = resultFlowInfo;
            boolean jumpOutPossible = false;
            for (ValueArgument argument: arguments) {
                KotlinTypeInfo argTypeInfo = context.trace.get(BindingContext.EXPRESSION_TYPE_INFO, argument.getArgumentExpression());
                if (argTypeInfo != null && argTypeInfo.getJumpOutPossible()) {
                    jumpOutPossible = true;
                    jumpFlowInfo = argTypeInfo.getJumpFlowInfo();
                    break;
                }
            }
            return TypeInfoFactoryKt.createTypeInfo(type, resultFlowInfo, jumpOutPossible, jumpFlowInfo);
        }

        KtExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof KtSimpleNameExpression && callExpression.getTypeArgumentList() == null) {
            TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                    context, "trace to resolve as variable with 'invoke' call", callExpression);
            KotlinType type = getVariableType((KtSimpleNameExpression) calleeExpression, receiver, callOperationNode,
                                              context.replaceTraceAndCache(temporaryForVariable), result);
            Qualifier qualifier = temporaryForVariable.trace.get(BindingContext.QUALIFIER, calleeExpression);
            if (result[0] && (qualifier == null || !(qualifier instanceof PackageQualifier))) {
                temporaryForVariable.commit();
                context.trace.report(FUNCTION_EXPECTED.on(calleeExpression, calleeExpression,
                                                          type != null ? type : ErrorUtils.createErrorType("")));
                return TypeInfoFactoryKt.noTypeInfo(context);
            }
        }
        temporaryForFunction.commit();
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    private static boolean canInstantiateAnnotationClass(@NotNull KtCallExpression expression, @NotNull BindingTrace trace) {
        //noinspection unchecked
        PsiElement parent = PsiTreeUtil.getParentOfType(expression, KtValueArgument.class, KtParameter.class);
        if (parent instanceof KtValueArgument) {
            if (PsiTreeUtil.getParentOfType(parent, KtAnnotationEntry.class) != null) {
                return true;
            }
            parent = PsiTreeUtil.getParentOfType(parent, KtParameter.class);
            if (parent != null) {
                return isUnderAnnotationClassDeclaration(trace, parent);
            }
        }
        else if (parent instanceof KtParameter) {
            return isUnderAnnotationClassDeclaration(trace, parent);
        }
        return false;
    }

    private static boolean isUnderAnnotationClassDeclaration(@NotNull BindingTrace trace, PsiElement parent) {
        KtClass ktClass = PsiTreeUtil.getParentOfType(parent, KtClass.class);
        if (ktClass != null) {
            DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktClass);
            return DescriptorUtils.isAnnotationClass(descriptor);
        }
        return false;
    }

    @NotNull
    private KotlinTypeInfo getSelectorReturnTypeInfo(
            @NotNull Receiver receiver,
            @Nullable ASTNode callOperationNode,
            @Nullable KtExpression selectorExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (selectorExpression instanceof KtCallExpression) {
            return getCallExpressionTypeInfoWithoutFinalTypeCheck((KtCallExpression) selectorExpression, receiver,
                                                                  callOperationNode, context);
        }
        else if (selectorExpression instanceof KtSimpleNameExpression) {
            return getSimpleNameExpressionTypeInfo((KtSimpleNameExpression) selectorExpression, receiver, callOperationNode, context);
        }
        else if (selectorExpression != null) {
            expressionTypingServices.getTypeInfo(selectorExpression, context);
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    public static void reportUnnecessarySafeCall(
            @NotNull BindingTrace trace, @NotNull KotlinType type,
            @NotNull ASTNode callOperationNode, @Nullable Receiver explicitReceiver
    ) {
        if (explicitReceiver instanceof ExpressionReceiver &&
            ((ExpressionReceiver) explicitReceiver).getExpression() instanceof KtSuperExpression) {
            trace.report(UNEXPECTED_SAFE_CALL.on(callOperationNode.getPsi()));
        }
        else {
            trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.getPsi(), type));
        }
    }

    /**
     * Visits a qualified expression like x.y or x?.z controlling data flow information changes.
     *
     * @return qualified expression type together with data flow information
     */
    @NotNull
    public KotlinTypeInfo getQualifiedExpressionTypeInfo(
            @NotNull KtQualifiedExpression expression, @NotNull final ExpressionTypingContext context
    ) {
        ExpressionTypingContext currentContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        BindingTrace trace = currentContext.trace;

        Function1<KtSimpleNameExpression, Boolean> isValueFunction = new Function1<KtSimpleNameExpression, Boolean>() {
            @Override
            public Boolean invoke(KtSimpleNameExpression nameExpression) {
                TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                        context, "trace to resolve as local variable or property", nameExpression);
                Call call = CallMaker.makePropertyCall(null, null, nameExpression);
                BasicCallResolutionContext contextForVariable = BasicCallResolutionContext.create(
                        context.replaceTraceAndCache(temporaryForVariable),
                        call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
                OverloadResolutionResults<VariableDescriptor> resolutionResult = callResolver.resolveSimpleProperty(contextForVariable);

                if (resolutionResult.isSingleResult() &&
                    resolutionResult.getResultingDescriptor() instanceof FakeCallableDescriptorForObject) {
                    return false;
                }

                OverloadResolutionResults.Code resultCode = resolutionResult.getResultCode();
                return resultCode != OverloadResolutionResults.Code.NAME_NOT_FOUND &&
                       resultCode != OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER;
            }
        };
        List<CallExpressionElement> elementChain =
                qualifiedExpressionResolver.resolveQualifierInExpressionAndUnroll(expression, context, isValueFunction);

        KotlinTypeInfo receiverTypeInfo;
        KotlinType receiverType;
        DataFlowInfo receiverDataFlowInfo;

        CallExpressionElement firstElement = elementChain.iterator().next();
        KtExpression firstReceiver = firstElement.getReceiver();
        Qualifier firstQualifier = trace.get(BindingContext.QUALIFIER, firstReceiver);
        if (firstQualifier == null) {
            receiverTypeInfo = expressionTypingServices.getTypeInfo(firstReceiver, currentContext);
            receiverType = receiverTypeInfo.getType();
            receiverDataFlowInfo = receiverTypeInfo.getDataFlowInfo();
        }
        else {
            receiverType = null;
            receiverDataFlowInfo = currentContext.dataFlowInfo;
            //noinspection ConstantConditions
            receiverTypeInfo = new KotlinTypeInfo(receiverType, receiverDataFlowInfo);
        }

        KotlinTypeInfo resultTypeInfo = receiverTypeInfo;

        boolean unconditional = true;
        DataFlowInfo unconditionalDataFlowInfo = receiverDataFlowInfo;

        for (CallExpressionElement element : elementChain) {
            if (receiverType == null) {
                receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());
            }
            QualifierReceiver qualifierReceiver = (QualifierReceiver) trace.get(BindingContext.QUALIFIER, element.getReceiver());

            Receiver receiver = qualifierReceiver == null
                                ? ExpressionReceiver.Companion.create(element.getReceiver(), receiverType, trace.getBindingContext())
                                : qualifierReceiver;

            boolean lastStage = element.getQualified() == expression;
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            ExpressionTypingContext contextForSelector = lastStage ? context : currentContext;
            if (TypeUtils.isNullableType(receiverType) && !element.getSafe()) {
                // Call with nullable receiver: take unconditional data flow info
                contextForSelector = contextForSelector.replaceDataFlowInfo(unconditionalDataFlowInfo);
            }
            else {
                // Take data flow info from the current receiver
                contextForSelector = contextForSelector.replaceDataFlowInfo(receiverDataFlowInfo);
            }

            if (receiver instanceof ReceiverValue) {
                DataFlowValue receiverDataFlowValue = DataFlowValueFactory.createDataFlowValue((ReceiverValue) receiver, context);
                // Additional "receiver != null" information
                // Should be applied if we consider a safe call
                if (element.getSafe()) {
                    DataFlowInfo dataFlowInfo = contextForSelector.dataFlowInfo;
                    if (dataFlowInfo.getPredictableNullability(receiverDataFlowValue).canBeNull()) {
                        contextForSelector = contextForSelector.replaceDataFlowInfo(
                                dataFlowInfo.disequate(receiverDataFlowValue, DataFlowValue.nullValue(builtIns)));
                    }
                    else {
                        reportUnnecessarySafeCall(trace, receiverType, element.getNode(), receiver);
                    }
                }
            }

            KtExpression selectorExpression = element.getSelector();
            KotlinTypeInfo selectorReturnTypeInfo =
                    getSelectorReturnTypeInfo(receiver, element.getNode(), selectorExpression, contextForSelector);
            KotlinType selectorReturnType = selectorReturnTypeInfo.getType();

            if (qualifierReceiver != null) {
                resolveDeferredReceiverInQualifiedExpression(qualifierReceiver, element.getQualified(), contextForSelector);
            }
            checkNestedClassAccess(element.getQualified(), contextForSelector);

            boolean safeCall = element.getSafe();
            if (safeCall && selectorReturnType != null && TypeUtils.isNullableType(receiverType)) {
                selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                selectorReturnTypeInfo = selectorReturnTypeInfo.replaceType(selectorReturnType);
            }

            // TODO : this is suspicious: remove this code?
            if (selectorExpression != null && selectorReturnType != null) {
                trace.recordType(selectorExpression, selectorReturnType);
            }
            resultTypeInfo = selectorReturnTypeInfo;
            CompileTimeConstant<?> value = constantExpressionEvaluator.evaluateExpression(
                    element.getQualified(), trace, contextForSelector.expectedType);
            if (value != null && value.isPure()) {
                resultTypeInfo =  dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, element.getQualified(), contextForSelector);
                if (lastStage) return resultTypeInfo;
            }
            if (contextForSelector.contextDependency == INDEPENDENT) {
                dataFlowAnalyzer.checkType(resultTypeInfo.getType(), element.getQualified(), contextForSelector);
            }
            // For the next stage, if any, current stage selector is the receiver!
            receiverTypeInfo = selectorReturnTypeInfo;
            receiverType = selectorReturnType;
            receiverDataFlowInfo = receiverTypeInfo.getDataFlowInfo();
            // if we have only dots and not ?. move unconditional data flow info further
            if (safeCall) {
                unconditional = false;
            }
            else if (unconditional) {
                unconditionalDataFlowInfo = receiverDataFlowInfo;
            }
            //noinspection ConstantConditions
            if (!lastStage && !trace.get(BindingContext.PROCESSED, element.getQualified())) {
                // Store type information (to prevent problems in call completer)
                trace.record(BindingContext.PROCESSED, element.getQualified());
                trace.record(BindingContext.EXPRESSION_TYPE_INFO, element.getQualified(),
                                            resultTypeInfo.replaceDataFlowInfo(unconditionalDataFlowInfo));
                // save scope before analyze and fix debugger: see CodeFragmentAnalyzer.correctContextForExpression
                BindingContextUtilsKt.recordScope(trace, contextForSelector.scope, element.getQualified());
                BindingContextUtilsKt.recordDataFlowInfo(contextForSelector.replaceDataFlowInfo(unconditionalDataFlowInfo),
                                                         element.getQualified());
            }
        }
        // if we are at last stage, we should just take result type info and set unconditional data flow info
        return resultTypeInfo.replaceDataFlowInfo(unconditionalDataFlowInfo);
    }

    private void resolveDeferredReceiverInQualifiedExpression(
            @NotNull QualifierReceiver qualifierReceiver,
            @NotNull KtQualifiedExpression qualifiedExpression,
            @NotNull ExpressionTypingContext context
    ) {
        KtExpression calleeExpression =
                KtPsiUtil.deparenthesize(CallUtilKt.getCalleeExpressionIfAny(qualifiedExpression.getSelectorExpression()));
        DeclarationDescriptor selectorDescriptor =
                calleeExpression instanceof KtReferenceExpression
                ? context.trace.get(BindingContext.REFERENCE_TARGET, (KtReferenceExpression) calleeExpression) : null;

        QualifiedExpressionResolveUtilKt.resolveQualifierAsReceiverInExpression(qualifierReceiver, selectorDescriptor, context, symbolUsageValidator);
    }

    private static void checkNestedClassAccess(
            @NotNull KtQualifiedExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        KtExpression selectorExpression = expression.getSelectorExpression();
        if (selectorExpression == null) return;

        // A.B - if B is a nested class accessed by outer class, 'A' and 'A.B' were marked as qualifiers
        // a.B - if B is a nested class accessed by instance reference, 'a.B' was marked as a qualifier, but 'a' was not (it's an expression)

        Qualifier expressionQualifier = context.trace.get(BindingContext.QUALIFIER, expression);
        Qualifier receiverQualifier = context.trace.get(BindingContext.QUALIFIER, expression.getReceiverExpression());

        if (receiverQualifier == null && expressionQualifier != null) {
            assert expressionQualifier instanceof ClassQualifier :
                    "Only class can (package cannot) be accessed by instance reference: " + expressionQualifier;
            context.trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.on(
                    selectorExpression,
                    ((ClassQualifier) expressionQualifier).getClassifier()));
        }
    }
}
