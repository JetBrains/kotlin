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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
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
import java.util.Deque;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class CallExpressionResolver {

    private final CallResolver callResolver;
    private final ConstantExpressionEvaluator constantExpressionEvaluator;
    private final SymbolUsageValidator symbolUsageValidator;
    private final DataFlowAnalyzer dataFlowAnalyzer;
    @NotNull private final KotlinBuiltIns builtIns;

    public CallExpressionResolver(
            @NotNull CallResolver callResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull SymbolUsageValidator symbolUsageValidator,
            @NotNull DataFlowAnalyzer dataFlowAnalyzer,
            @NotNull KotlinBuiltIns builtIns
    ) {
        this.callResolver = callResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.symbolUsageValidator = symbolUsageValidator;
        this.dataFlowAnalyzer = dataFlowAnalyzer;
        this.builtIns = builtIns;
    }

    private ExpressionTypingServices expressionTypingServices;

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Nullable
    public ResolvedCall<FunctionDescriptor> getResolvedCallForFunction(
            @NotNull Call call, @NotNull KtExpression callExpression,
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
            @NotNull KtSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
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
            boolean isQualifier = isLHSOfDot && resolutionResult.isSingleResult()
                                  && resolutionResult.getResultingDescriptor() instanceof FakeCallableDescriptorForObject;
            if (!isQualifier) {
                result[0] = true;
                temporaryForVariable.commit();
                return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
            }
        }

        QualifierReceiver qualifier = QualifierKt.createQualifier(nameExpression, receiver, context);
        if (qualifier != null) {
            result[0] = true;
            if (!isLHSOfDot) {
                QualifierKt.resolveAsStandaloneExpression(qualifier, context, symbolUsageValidator);
            }
            return null;
        }
        temporaryForVariable.commit();
        result[0] = !resolutionResult.isNothing();
        return resolutionResult.isSingleResult() ? resolutionResult.getResultingDescriptor().getReturnType() : null;
    }

    @NotNull
    public KotlinTypeInfo getSimpleNameExpressionTypeInfo(
            @NotNull KtSimpleNameExpression nameExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];

        TemporaryTraceAndCache temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolve as variable", nameExpression);
        KotlinType type =
                getVariableType(nameExpression, receiver, callOperationNode, context.replaceTraceAndCache(temporaryForVariable), result);
        // NB: we have duplicating code in ArgumentTypeResolver.
        // It would be better to do it in getSelectorTypeInfo, but it breaks call expression analysis
        // (all safe calls become unnecessary after it)
        // QualifierReceiver is a thing like Collections. which has no type or value
        if (receiver.exists() && !(receiver instanceof QualifierReceiver)) {
            DataFlowValue receiverDataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, context);
            if (callOperationNode != null && callOperationNode.getElementType() == KtTokens.SAFE_ACCESS) {
                context = context.replaceDataFlowInfo(context.dataFlowInfo.disequate(receiverDataFlowValue, DataFlowValue.nullValue(builtIns)));
            }
        }

        if (result[0]) {
            temporaryForVariable.commit();
            return TypeInfoFactoryKt.createTypeInfo(type, context);
        }

        Call call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, Collections.<ValueArgument>emptyList());
        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function", nameExpression);
        ResolutionContext newContext = context.replaceTraceAndCache(temporaryForFunction);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, nameExpression, newContext, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            temporaryForFunction.commit();
            boolean hasValueParameters = functionDescriptor == null || functionDescriptor.getValueParameters().size() > 0;
            context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters));
            type = functionDescriptor != null ? functionDescriptor.getReturnType() : null;
            return TypeInfoFactoryKt.createTypeInfo(type, context);
        }

        temporaryForVariable.commit();
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @NotNull
    public KotlinTypeInfo getCallExpressionTypeInfo(
            @NotNull KtCallExpression callExpression, @NotNull ReceiverValue receiver,
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
    public KotlinTypeInfo getCallExpressionTypeInfoWithoutFinalTypeCheck(
            @NotNull KtCallExpression callExpression, @NotNull ReceiverValue receiver,
            @Nullable ASTNode callOperationNode, @NotNull ExpressionTypingContext context
    ) {
        boolean[] result = new boolean[1];
        Call call = CallMaker.makeCall(receiver, callOperationNode, callExpression);

        TemporaryTraceAndCache temporaryForFunction = TemporaryTraceAndCache.create(
                context, "trace to resolve as function call", callExpression);
        ResolvedCall<FunctionDescriptor> resolvedCall = getResolvedCallForFunction(
                call, callExpression,
                // It's possible start of a call so we should reset safe call chain
                context.replaceTraceAndCache(temporaryForFunction).replaceInsideCallChain(false),
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, result);
        if (result[0]) {
            FunctionDescriptor functionDescriptor = resolvedCall != null ? resolvedCall.getResultingDescriptor() : null;
            temporaryForFunction.commit();
            if (callExpression.getValueArgumentList() == null && callExpression.getFunctionLiteralArguments().isEmpty()) {
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
            if (result[0] && (qualifier == null || qualifier.getPackageView() == null)) {
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
            return PsiTreeUtil.getParentOfType(parent, KtAnnotationEntry.class) != null;
        }
        else if (parent instanceof KtParameter) {
            KtClass ktClass = PsiTreeUtil.getParentOfType(parent, KtClass.class);
            if (ktClass != null) {
                DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktClass);
                return DescriptorUtils.isAnnotationClass(descriptor);
            }
        }
        return false;
    }

    @NotNull
    private KotlinTypeInfo getSelectorReturnTypeInfo(
            @NotNull ReceiverValue receiver,
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
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression, selectorExpression.getText()));
        }
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    /**
     * Visits a qualified expression like x.y or x?.z controlling data flow information changes.
     *
     * @return qualified expression type together with data flow information
     */
    @NotNull
    public KotlinTypeInfo getQualifiedExpressionTypeInfo(
            @NotNull KtQualifiedExpression expression, @NotNull ExpressionTypingContext context
    ) {
        ExpressionTypingContext currentContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        Deque<CallExpressionElement> elementChain = CallExpressionUnrollerKt.unroll(expression);

        KotlinTypeInfo receiverTypeInfo = expressionTypingServices.getTypeInfo(elementChain.getFirst().getReceiver(), currentContext);
        KotlinType receiverType = receiverTypeInfo.getType();
        DataFlowInfo receiverDataFlowInfo = receiverTypeInfo.getDataFlowInfo();
        KotlinTypeInfo resultTypeInfo = receiverTypeInfo;

        boolean unconditional = true;
        DataFlowInfo unconditionalDataFlowInfo = receiverDataFlowInfo;

        for (CallExpressionElement element : elementChain) {
            if (receiverType == null) {
                receiverType = ErrorUtils.createErrorType("Type for " + expression.getText());
            }
            QualifierReceiver qualifierReceiver = (QualifierReceiver) context.trace.get(BindingContext.QUALIFIER, element.getReceiver());

            ReceiverValue receiver = qualifierReceiver == null ?
                                     ExpressionReceiver.create(element.getReceiver(), receiverType, context.trace.getBindingContext()) :
                                     qualifierReceiver;

            boolean lastStage = element.getQualified() == expression;
            assert lastStage == (element == elementChain.getLast());
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            // But receiver data flow info changes should be always applied, while we are inside call chain
            ExpressionTypingContext baseContext = lastStage ? context : currentContext;
            currentContext = baseContext.replaceDataFlowInfo(receiverDataFlowInfo);

            KtExpression selectorExpression = element.getSelector();
            KotlinTypeInfo selectorReturnTypeInfo =
                    getSelectorReturnTypeInfo(receiver, element.getNode(), selectorExpression, currentContext);
            KotlinType selectorReturnType = selectorReturnTypeInfo.getType();

            resolveDeferredReceiverInQualifiedExpression(qualifierReceiver, element.getQualified(), currentContext);
            checkNestedClassAccess(element.getQualified(), currentContext);

            boolean safeCall = element.getSafe();
            if (safeCall && selectorReturnType != null && TypeUtils.isNullableType(receiverType)) {
                selectorReturnType = TypeUtils.makeNullable(selectorReturnType);
                selectorReturnTypeInfo = selectorReturnTypeInfo.replaceType(selectorReturnType);
            }

            // TODO : this is suspicious: remove this code?
            if (selectorExpression != null && selectorReturnType != null) {
                currentContext.trace.recordType(selectorExpression, selectorReturnType);
            }
            resultTypeInfo = selectorReturnTypeInfo;
            CompileTimeConstant<?> value = constantExpressionEvaluator.evaluateExpression(element.getQualified(), currentContext.trace, currentContext.expectedType);
            if (value != null && value.isPure()) {
                resultTypeInfo =  dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, element.getQualified(), currentContext);
                if (lastStage) return resultTypeInfo;
            }
            if (currentContext.contextDependency == INDEPENDENT) {
                dataFlowAnalyzer.checkType(resultTypeInfo.getType(), element.getQualified(), currentContext);
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
            if (!lastStage && !currentContext.trace.get(BindingContext.PROCESSED, element.getQualified())) {
                // Store type information (to prevent problems in call completer)
                currentContext.trace.record(BindingContext.PROCESSED, element.getQualified());
                currentContext.trace.record(BindingContext.EXPRESSION_TYPE_INFO, element.getQualified(),
                                            resultTypeInfo.replaceDataFlowInfo(unconditionalDataFlowInfo));
                // save scope before analyze and fix debugger: see CodeFragmentAnalyzer.correctContextForExpression
                BindingContextUtilsKt.recordScope(currentContext.trace, currentContext.scope, element.getQualified());
                BindingContextUtilsKt.recordDataFlowInfo(currentContext.replaceDataFlowInfo(unconditionalDataFlowInfo), element.getQualified());
            }
        }
        // if we are at last stage, we should just take result type info and set unconditional data flow info
        return resultTypeInfo.replaceDataFlowInfo(unconditionalDataFlowInfo);
    }

    private void resolveDeferredReceiverInQualifiedExpression(
            @Nullable QualifierReceiver qualifierReceiver,
            @NotNull KtQualifiedExpression qualifiedExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (qualifierReceiver == null) return;
        KtExpression calleeExpression =
                KtPsiUtil.deparenthesize(CallUtilKt.getCalleeExpressionIfAny(qualifiedExpression.getSelectorExpression()));
        DeclarationDescriptor selectorDescriptor =
                calleeExpression instanceof KtReferenceExpression
                ? context.trace.get(BindingContext.REFERENCE_TARGET, (KtReferenceExpression) calleeExpression) : null;
        QualifierKt.resolveAsReceiverInQualifiedExpression(qualifierReceiver, context, symbolUsageValidator, selectorDescriptor);
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
            assert expressionQualifier.getClassifier() instanceof ClassDescriptor :
                    "Only class can (package cannot) be accessed by instance reference: " + expressionQualifier;
            context.trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE
                                         .on(selectorExpression, (ClassDescriptor) expressionQualifier.getClassifier()));
        }
    }
}
