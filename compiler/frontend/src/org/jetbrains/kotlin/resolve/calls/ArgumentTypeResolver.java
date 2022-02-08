/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls;

import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.builtins.UnsignedTypes;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.util.ResolveArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImplKt;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.*;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.psi.KtPsiUtil.getLastElementDeparenthesized;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getRecordedTypeInfo;
import static org.jetbrains.kotlin.resolve.calls.util.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.DONT_CARE;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {
    @NotNull private final TypeResolver typeResolver;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final ReflectionTypes reflectionTypes;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;
    @NotNull private final FunctionPlaceholders functionPlaceholders;
    @NotNull private final ModuleDescriptor moduleDescriptor;
    @NotNull private final KotlinTypeChecker kotlinTypeChecker;

    private ExpressionTypingServices expressionTypingServices;
    private DoubleColonExpressionResolver doubleColonExpressionResolver;

    public ArgumentTypeResolver(
            @NotNull TypeResolver typeResolver,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull ReflectionTypes reflectionTypes,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull FunctionPlaceholders functionPlaceholders,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull KotlinTypeChecker kotlinTypeChecker
    ) {
        this.typeResolver = typeResolver;
        this.builtIns = builtIns;
        this.reflectionTypes = reflectionTypes;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.functionPlaceholders = functionPlaceholders;
        this.moduleDescriptor = moduleDescriptor;
        this.kotlinTypeChecker = kotlinTypeChecker;
    }

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setDoubleColonExpressionResolver(@NotNull DoubleColonExpressionResolver doubleColonExpressionResolver) {
        this.doubleColonExpressionResolver = doubleColonExpressionResolver;
    }

    public boolean isSubtypeOfForArgumentType(
            @NotNull KotlinType actualType,
            @NotNull KotlinType expectedType
    ) {
        if (FunctionPlaceholdersKt.isFunctionPlaceholder(actualType)) {
            KotlinType functionType = ConstraintSystemBuilderImplKt.createTypeForFunctionPlaceholder(actualType, expectedType);
            return kotlinTypeChecker.isSubtypeOf(functionType, expectedType);
        }
        return kotlinTypeChecker.isSubtypeOf(actualType, expectedType);
    }

    public void checkTypesWithNoCallee(
            @NotNull CallResolutionContext<?> context
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            KtExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof KtLambdaExpression)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }

        checkTypesForFunctionArgumentsWithNoCallee(context);

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

    private void checkTypesForFunctionArgumentsWithNoCallee(@NotNull CallResolutionContext<?> context) {
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
        return isFunctionLiteralArgument(expression, context.statementFilter);
    }

    private static boolean isFunctionLiteralArgument(
            @NotNull KtExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return getFunctionLiteralArgumentIfAny(expression, statementFilter) != null;
    }

    public static boolean isCallableReferenceArgument(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        return isCallableReferenceArgument(expression, context.statementFilter);
    }

    private static boolean isCallableReferenceArgument(
            @NotNull KtExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return getCallableReferenceExpressionIfAny(expression, statementFilter) != null;
    }

    public static boolean isFunctionLiteralOrCallableReference(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        return isFunctionLiteralOrCallableReference(expression, context.statementFilter);
    }

    public static boolean isFunctionLiteralOrCallableReference(
            @NotNull KtExpression expression, @NotNull StatementFilter statementFilter
    ) {
        return isFunctionLiteralArgument(expression, statementFilter) || isCallableReferenceArgument(expression, statementFilter);
    }

    @Nullable
    public static KtFunction getFunctionLiteralArgumentIfAny(
            @NotNull KtExpression expression, @NotNull ResolutionContext context
    ) {
        return getFunctionLiteralArgumentIfAny(expression, context.statementFilter);
    }

    @Nullable
    public static KtFunction getFunctionLiteralArgumentIfAny(
            @NotNull KtExpression expression, @NotNull StatementFilter statementFilter
    ) {
        KtExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, statementFilter);
        if (deparenthesizedExpression instanceof KtLambdaExpression) {
            return ((KtLambdaExpression) deparenthesizedExpression).getFunctionLiteral();
        }
        if (deparenthesizedExpression instanceof KtFunction) {
            return (KtFunction) deparenthesizedExpression;
        }
        return null;
    }

    @Nullable
    public static KtCallableReferenceExpression getCallableReferenceExpressionIfAny(
            @NotNull KtExpression expression,
            @NotNull ResolutionContext context
    ) {
        return getCallableReferenceExpressionIfAny(expression, context.statementFilter);
    }

    @Nullable
    public static KtCallableReferenceExpression getCallableReferenceExpressionIfAny(
            @NotNull KtExpression expression,
            @NotNull StatementFilter statementFilter
    ) {
        KtExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, statementFilter);
        if (deparenthesizedExpression instanceof KtCallableReferenceExpression) {
            return (KtCallableReferenceExpression) deparenthesizedExpression;
        }
        return null;
    }

    @NotNull
    public KotlinTypeInfo getArgumentTypeInfo(
            @Nullable KtExpression expression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode,
            boolean suspendFunctionTypeExpected
    ) {
        if (expression == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }

        KtFunction functionLiteralArgument = getFunctionLiteralArgumentIfAny(expression, context);
        if (functionLiteralArgument != null) {
            return getFunctionLiteralTypeInfo(expression, functionLiteralArgument, context, resolveArgumentsMode, suspendFunctionTypeExpected);
        }

        KtCallableReferenceExpression callableReferenceExpression = getCallableReferenceExpressionIfAny(expression, context);
        if (callableReferenceExpression != null) {
            return getCallableReferenceTypeInfo(expression, callableReferenceExpression, context, resolveArgumentsMode);
        }

        if (isCollectionLiteralInsideAnnotation(expression, context)) {
            // We assume that there is only one candidate resolver for annotation call
            // And to resolve collection literal correctly, we need mapping of argument to parameter to get expected type and
            // to choose corresponding call (i.e arrayOf/intArrayOf...)
            ResolutionContext newContext = context.replaceContextDependency(INDEPENDENT);
            return expressionTypingServices.getTypeInfo(expression, newContext);
        }

        // TODO: probably should be "is unsigned type or is supertype of unsigned type" to support Comparable<UInt> expected types too
        if (UnsignedTypes.INSTANCE.isUnsignedType(context.expectedType)) {
            convertSignedConstantToUnsigned(expression, context);
        }

        KotlinTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }

        ResolutionContext newContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(DEPENDENT);

        return expressionTypingServices.getTypeInfo(expression, newContext);
    }

    private void convertSignedConstantToUnsigned(
            @NotNull KtExpression expression,
            @NotNull CallResolutionContext<?> context
    ) {
        CompileTimeConstant<?> constant = context.trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
        if (!(constant instanceof IntegerValueTypeConstant) || !constantCanBeConvertedToUnsigned(constant)) return;

        IntegerValueTypeConstant unsignedConstant = IntegerValueTypeConstant.convertToUnsignedConstant(
                (IntegerValueTypeConstant) constant, moduleDescriptor
        );

        context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, unsignedConstant);

        updateResultArgumentTypeIfNotDenotable(
                context.trace, context.statementFilter, context.expectedType, unsignedConstant.getUnknownIntegerType(), expression
        );
    }

    public static boolean constantCanBeConvertedToUnsigned(@NotNull CompileTimeConstant<?> constant) {
        return !constant.isError() && constant.getParameters().isPure();
    }

    @NotNull
    public KotlinTypeInfo getCallableReferenceTypeInfo(
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
        Pair<DoubleColonLHS, OverloadResolutionResults<?>> pair =
                doubleColonExpressionResolver.resolveCallableReference(
                        callableReferenceExpression,
                        ExpressionTypingContext.newContext(context),
                        SHAPE_FUNCTION_ARGUMENTS
                );
        DoubleColonLHS lhs = pair.getFirst();
        OverloadResolutionResults<?> overloadResolutionResults = pair.getSecond();

        if (overloadResolutionResults == null) return null;

        if (isSingleAndPossibleTransformToSuccess(overloadResolutionResults)) {
            ResolvedCall<?> resolvedCall = OverloadResolutionResultsUtil.getResultingCall(overloadResolutionResults, context);
            if (resolvedCall == null) return null;

            return DoubleColonExpressionResolver.Companion.createKCallableTypeForReference(
                    resolvedCall.getResultingDescriptor(), lhs, reflectionTypes, context.scope.getOwnerDescriptor()
            );
        }

        if (expectedTypeIsUnknown) {
            return functionPlaceholders.createFunctionPlaceholderType(Collections.emptyList(), false);
        }

        return FunctionTypesKt.createFunctionType(
                builtIns, Annotations.Companion.getEMPTY(), null, Collections.emptyList(), Collections.emptyList(), null, TypeUtils.DONT_CARE
        );
    }

    private static boolean isSingleAndPossibleTransformToSuccess(@NotNull OverloadResolutionResults<?> overloadResolutionResults) {
        if (!overloadResolutionResults.isSingleResult()) return false;
        ResolvedCall<?> call = CollectionsKt.singleOrNull(overloadResolutionResults.getResultingCalls());
        return call != null && call.getStatus().possibleTransformToSuccess();
    }

    @NotNull
    public KotlinTypeInfo getFunctionLiteralTypeInfo(
            @NotNull KtExpression expression,
            @NotNull KtFunction functionLiteral,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode,
            boolean suspendFunctionTypeExpected
    ) {
        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
            KotlinType type = getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, true, suspendFunctionTypeExpected);
            return TypeInfoFactoryKt.createTypeInfo(type, context);
        }
        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(INDEPENDENT));
    }

    @Nullable
    public KotlinType getShapeTypeOfFunctionLiteral(
            @NotNull KtFunction function,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            boolean expectedTypeIsUnknown,
            boolean suspendFunctionTypeExpected
    ) {
        boolean isFunctionLiteral = function instanceof KtFunctionLiteral;
        if (function.getValueParameterList() == null && isFunctionLiteral) {
            return expectedTypeIsUnknown
                   ? functionPlaceholders.createFunctionPlaceholderType(Collections.emptyList(), /* hasDeclaredArguments = */ false)
                   : FunctionTypesKt.createFunctionType(
                           builtIns, Annotations.Companion.getEMPTY(), null, Collections.emptyList(), Collections.emptyList(), null, DONT_CARE
                   );
        }
        List<KtParameter> valueParameters = function.getValueParameters();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<KotlinType> parameterTypes = new ArrayList<>(valueParameters.size());
        List<Name> parameterNames = new ArrayList<>(valueParameters.size());
        for (KtParameter parameter : valueParameters) {
            parameterTypes.add(resolveTypeRefWithDefault(parameter.getTypeReference(), scope, temporaryTrace, DONT_CARE));
            Name name = parameter.getNameAsName();
            if (name == null) {
                name = SpecialNames.NO_NAME_PROVIDED;
            }
            parameterNames.add(name);
        }
        KotlinType returnType = resolveTypeRefWithDefault(function.getTypeReference(), scope, temporaryTrace, DONT_CARE);
        assert returnType != null;
        KotlinType receiverType = resolveTypeRefWithDefault(function.getReceiverTypeReference(), scope, temporaryTrace, null);
        List<KotlinType> contextReceiversTypes = function.getContextReceivers().stream().map(contextReceiver ->
            resolveTypeRefWithDefault(contextReceiver.typeReference(), scope, temporaryTrace, null)
        ).collect(Collectors.toList());

        return expectedTypeIsUnknown && isFunctionLiteral
               ? functionPlaceholders.createFunctionPlaceholderType(parameterTypes, /* hasDeclaredArguments = */ true)
               : FunctionTypesKt.createFunctionType(
                       builtIns, Annotations.Companion.getEMPTY(), receiverType, contextReceiversTypes, parameterTypes, parameterNames, returnType, suspendFunctionTypeExpected
               );
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
            @NotNull CallResolutionContext<?> context, @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        MutableDataFlowInfoForArguments infoForArguments = context.dataFlowInfoForArguments;
        Call call = context.call;

        for (ValueArgument argument : call.getValueArguments()) {
            KtExpression expression = argument.getArgumentExpression();
            if (expression == null) continue;

            if (isCollectionLiteralInsideAnnotation(expression, context)) {
                continue;
            }

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            // Here we go inside arguments and determine additional data flow information for them
            KotlinTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, resolveArgumentsMode, false);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
        }
    }

    @Nullable
    public KotlinType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull KtExpression expression
    ) {
        return updateResultArgumentTypeIfNotDenotable(context.trace, context.statementFilter, context.expectedType, expression);
    }

    @Nullable
    public KotlinType updateResultArgumentTypeIfNotDenotable(
            @NotNull BindingTrace trace,
            @NotNull StatementFilter statementFilter,
            @NotNull KotlinType expectedType,
            @NotNull KtExpression expression
    ) {
        KotlinType type = trace.getType(expression);
        return type != null ? updateResultArgumentTypeIfNotDenotable(trace, statementFilter, expectedType, type, expression) : null;
    }

    @Nullable
    public KotlinType updateResultArgumentTypeIfNotDenotable(
            @NotNull BindingTrace trace,
            @NotNull StatementFilter statementFilter,
            @NotNull KotlinType expectedType,
            @NotNull KotlinType targetType,
            @NotNull KtExpression expression
    ) {
        TypeConstructor typeConstructor = targetType.getConstructor();
        if (!typeConstructor.isDenotable()) {
            if (typeConstructor instanceof IntegerValueTypeConstructor) {
                IntegerValueTypeConstructor constructor = (IntegerValueTypeConstructor) typeConstructor;
                KotlinType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, expectedType);
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace);
                return primitiveType;
            }
            if (typeConstructor instanceof IntegerLiteralTypeConstructor) {
                IntegerLiteralTypeConstructor constructor = (IntegerLiteralTypeConstructor) typeConstructor;
                KotlinType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, expectedType);
                constantExpressionEvaluator.updateNumberType(primitiveType, expression, statementFilter, trace);
                return primitiveType;
            }
        }
        return null;
    }

    private static boolean isCollectionLiteralInsideAnnotation(KtExpression expression, CallResolutionContext<?> context) {
        return expression instanceof KtCollectionLiteralExpression && context.call.getCallElement() instanceof KtAnnotationEntry;
    }
}
