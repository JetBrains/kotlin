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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.callableReferences.CallableReferencesResolutionUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImplKt;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.FunctionPlaceholders;
import org.jetbrains.kotlin.types.FunctionPlaceholdersKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.JetTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.psi.KtPsiUtil.getLastElementDeparenthesized;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getRecordedTypeInfo;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.DONT_CARE;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {
    @NotNull private final TypeResolver typeResolver;
    @NotNull private final CallResolver callResolver;
    @NotNull private final ExpressionTypingServices expressionTypingServices;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final ReflectionTypes reflectionTypes;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;
    @NotNull private final FunctionPlaceholders functionPlaceholders;

    public ArgumentTypeResolver(
            @NotNull TypeResolver typeResolver,
            @NotNull CallResolver callResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull ReflectionTypes reflectionTypes,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull FunctionPlaceholders functionPlaceholders
    ) {
        this.typeResolver = typeResolver;
        this.callResolver = callResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.builtIns = builtIns;
        this.reflectionTypes = reflectionTypes;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.functionPlaceholders = functionPlaceholders;
    }

    public static boolean isSubtypeOfForArgumentType(
            @NotNull KotlinType actualType,
            @NotNull KotlinType expectedType
    ) {
        if (FunctionPlaceholdersKt.isFunctionPlaceholder(actualType)) {
            KotlinType functionType = ConstraintSystemImplKt.createTypeForFunctionPlaceholder(actualType, expectedType);
            return KotlinTypeChecker.DEFAULT.isSubtypeOf(functionType, expectedType);
        }
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(actualType, expectedType);
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context) {
        checkTypesWithNoCallee(context, SHAPE_FUNCTION_ARGUMENTS);
    }

    public void checkTypesWithNoCallee(
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveFunctionArgumentBodies
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            KtExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof KtFunctionLiteralExpression)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }

        if (resolveFunctionArgumentBodies == RESOLVE_FUNCTION_ARGUMENTS) {
            checkTypesForFunctionArgumentsWithNoCallee(context);
        }

        for (KtTypeProjection typeProjection : context.call.getTypeArguments()) {
            KtTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    public void checkTypesForFunctionArgumentsWithNoCallee(@NotNull CallResolutionContext<?> context) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            KtExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && isFunctionLiteralArgument(argumentExpression, context)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }
    }

    private void checkArgumentTypeWithNoCallee(CallResolutionContext<?> context, KtExpression argumentExpression) {
        expressionTypingServices.getTypeInfo(argumentExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression);
    }

    public static boolean isFunctionLiteralArgument(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        return getFunctionLiteralArgumentIfAny(expression, context) != null;
    }

    @NotNull
    public static KtFunction getFunctionLiteralArgument(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        assert isFunctionLiteralArgument(expression, context);
        //noinspection ConstantConditions
        return getFunctionLiteralArgumentIfAny(expression, context);
    }

    @Nullable
    public static KtFunction getFunctionLiteralArgumentIfAny(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        KtExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, context.statementFilter);
        if (deparenthesizedExpression instanceof KtFunctionLiteralExpression) {
            return ((KtFunctionLiteralExpression) deparenthesizedExpression).getFunctionLiteral();
        }
        if (deparenthesizedExpression instanceof KtFunction) {
            return (KtFunction) deparenthesizedExpression;
        }
        return null;
    }

    @Nullable
    public static KtCallableReferenceExpression getCallableReferenceExpressionIfAny(
            @NotNull KtExpression expression,
            @NotNull CallResolutionContext<?> context
    ) {
        KtExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, context.statementFilter);
        if (deparenthesizedExpression instanceof KtCallableReferenceExpression) {
            return (KtCallableReferenceExpression) deparenthesizedExpression;
        }
        return null;
    }

    @NotNull
    public JetTypeInfo getArgumentTypeInfo(
            @Nullable KtExpression expression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (expression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        KtFunction functionLiteralArgument = getFunctionLiteralArgumentIfAny(expression, context);
        if (functionLiteralArgument != null) {
            return getFunctionLiteralTypeInfo(expression, functionLiteralArgument, context, resolveArgumentsMode);
        }

        KtCallableReferenceExpression callableReferenceExpression = getCallableReferenceExpressionIfAny(expression, context);
        if (callableReferenceExpression != null) {
            return getCallableReferenceTypeInfo(expression, callableReferenceExpression, context, resolveArgumentsMode);
        }

        JetTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }

        ResolutionContext newContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(DEPENDENT);

        return expressionTypingServices.getTypeInfo(expression, newContext);
    }

    @NotNull
    public JetTypeInfo getCallableReferenceTypeInfo(
            @NotNull KtExpression expression,
            @NotNull KtCallableReferenceExpression callableReferenceExpression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
            KotlinType type = getShapeTypeOfCallableReference(callableReferenceExpression, context, true);
            return TypeInfoFactoryKt.createTypeInfo(type);
        }
        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(INDEPENDENT));
    }

    @Nullable
    public KotlinType getShapeTypeOfCallableReference(
            @NotNull KtCallableReferenceExpression callableReferenceExpression,
            @NotNull CallResolutionContext<?> context,
            boolean expectedTypeIsUnknown
    ) {
        KotlinType receiverType =
                CallableReferencesResolutionUtilsKt.resolveCallableReferenceReceiverType(callableReferenceExpression, context, typeResolver);
        OverloadResolutionResults<CallableDescriptor> overloadResolutionResults =
                CallableReferencesResolutionUtilsKt.resolvePossiblyAmbiguousCallableReference(
                        callableReferenceExpression, receiverType, context, ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS,
                        callResolver);
        return CallableReferencesResolutionUtilsKt.getResolvedCallableReferenceShapeType(
                callableReferenceExpression, receiverType, overloadResolutionResults, context, expectedTypeIsUnknown,
                reflectionTypes, builtIns, functionPlaceholders);
    }

    @NotNull
    public JetTypeInfo getFunctionLiteralTypeInfo(
            @NotNull KtExpression expression,
            @NotNull KtFunction functionLiteral,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
            KotlinType type = getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, true);
            return TypeInfoFactoryKt.createTypeInfo(type, context);
        }
        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(INDEPENDENT));
    }

    @Nullable
    public KotlinType getShapeTypeOfFunctionLiteral(
            @NotNull KtFunction function,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            boolean expectedTypeIsUnknown
    ) {
        boolean isFunctionLiteral = function instanceof KtFunctionLiteral;
        if (function.getValueParameterList() == null && isFunctionLiteral) {
            return expectedTypeIsUnknown
                   ? functionPlaceholders
                           .createFunctionPlaceholderType(Collections.<KotlinType>emptyList(), /* hasDeclaredArguments = */ false)
                   : builtIns.getFunctionType(Annotations.Companion.getEMPTY(), null, Collections.<KotlinType>emptyList(), DONT_CARE);
        }
        List<KtParameter> valueParameters = function.getValueParameters();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<KotlinType> parameterTypes = Lists.newArrayList();
        for (KtParameter parameter : valueParameters) {
            parameterTypes.add(resolveTypeRefWithDefault(parameter.getTypeReference(), scope, temporaryTrace, DONT_CARE));
        }
        KotlinType returnType = resolveTypeRefWithDefault(function.getTypeReference(), scope, temporaryTrace, DONT_CARE);
        assert returnType != null;
        KotlinType receiverType = resolveTypeRefWithDefault(function.getReceiverTypeReference(), scope, temporaryTrace, null);

        return expectedTypeIsUnknown && isFunctionLiteral
               ? functionPlaceholders.createFunctionPlaceholderType(parameterTypes, /* hasDeclaredArguments = */ true)
               : builtIns.getFunctionType(Annotations.Companion.getEMPTY(), receiverType, parameterTypes, returnType);
    }

    @Nullable
    public KotlinType resolveTypeRefWithDefault(
            @Nullable KtTypeReference returnTypeRef,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @Nullable KotlinType defaultValue
    ) {
        if (returnTypeRef != null) {
            return typeResolver.resolveType(scope, returnTypeRef, trace, true);
        }
        return defaultValue;
    }

    /**
     * Visits function call arguments and determines data flow information changes
     */
    public void analyzeArgumentsAndRecordTypes(
            @NotNull CallResolutionContext<?> context
    ) {
        MutableDataFlowInfoForArguments infoForArguments = context.dataFlowInfoForArguments;
        Call call = context.call;
        ReceiverValue receiver = call.getExplicitReceiver();
        DataFlowInfo initialDataFlowInfo = context.dataFlowInfo;
        // QualifierReceiver is a thing like Collections. which has no type or value
        if (receiver.exists() && !(receiver instanceof QualifierReceiver)) {
            DataFlowValue receiverDataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, context);
            // Additional "receiver != null" information for KT-5840
            // Should be applied if we consider a safe call
            // For an unsafe call, we should not do it,
            // otherwise not-null will propagate to successive statements
            // Sample: x?.foo(x.bar()) // Inside foo call, x is not-nullable
            if (CallUtilKt.isSafeCall(call)) {
                initialDataFlowInfo = initialDataFlowInfo.disequate(receiverDataFlowValue, DataFlowValue.nullValue(builtIns));
            }
        }
        infoForArguments.setInitialDataFlowInfo(initialDataFlowInfo);

        for (ValueArgument argument : call.getValueArguments()) {
            KtExpression expression = argument.getArgumentExpression();
            if (expression == null) continue;

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            // Here we go inside arguments and determine additional data flow information for them
            JetTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, SHAPE_FUNCTION_ARGUMENTS);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
        }
    }

    @Nullable
    public KotlinType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull KtExpression expression
    ) {
        KotlinType type = context.trace.getType(expression);
        if (type != null && !type.getConstructor().isDenotable()) {
            if (type.getConstructor() instanceof IntegerValueTypeConstructor) {
                IntegerValueTypeConstructor constructor = (IntegerValueTypeConstructor) type.getConstructor();
                KotlinType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, context.expectedType);
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, context.statementFilter, context.trace);
                return primitiveType;
            }
        }
        return null;
    }
}
