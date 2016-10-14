/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.ReflectionTypes;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImplKt;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.util.FunctionTypeResolveUtilsKt;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.FunctionPlaceholders;
import org.jetbrains.kotlin.types.FunctionPlaceholdersKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.*;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import javax.inject.Inject;
import java.util.ArrayList;
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
    @NotNull private final DoubleColonExpressionResolver doubleColonExpressionResolver;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final ReflectionTypes reflectionTypes;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;
    @NotNull private final FunctionPlaceholders functionPlaceholders;

    private ExpressionTypingServices expressionTypingServices;

    public ArgumentTypeResolver(
            @NotNull TypeResolver typeResolver,
            @NotNull DoubleColonExpressionResolver doubleColonExpressionResolver,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull ReflectionTypes reflectionTypes,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull FunctionPlaceholders functionPlaceholders
    ) {
        this.typeResolver = typeResolver;
        this.doubleColonExpressionResolver = doubleColonExpressionResolver;
        this.builtIns = builtIns;
        this.reflectionTypes = reflectionTypes;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.functionPlaceholders = functionPlaceholders;
    }

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    public static boolean isSubtypeOfForArgumentType(
            @NotNull KotlinType actualType,
            @NotNull KotlinType expectedType
    ) {
        if (FunctionPlaceholdersKt.isFunctionPlaceholder(actualType)) {
            KotlinType functionType = ConstraintSystemBuilderImplKt.createTypeForFunctionPlaceholder(actualType, expectedType);
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
            if (argumentExpression != null && !(argumentExpression instanceof KtLambdaExpression)) {
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
            @NotNull CallResolutionContext<?> context
    ) {
        KtExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, context.statementFilter);
        if (deparenthesizedExpression instanceof KtCallableReferenceExpression) {
            return (KtCallableReferenceExpression) deparenthesizedExpression;
        }
        return null;
    }

    @NotNull
    public KotlinTypeInfo getArgumentTypeInfo(
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

        KotlinTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }

        ResolutionContext newContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(DEPENDENT);

        return expressionTypingServices.getTypeInfo(expression, newContext);
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

        if (overloadResolutionResults.isSingleResult()) {
            ResolvedCall<?> resolvedCall =
                    OverloadResolutionResultsUtil.getResultingCall(overloadResolutionResults, context.contextDependency);
            if (resolvedCall == null) return null;

            return DoubleColonExpressionResolver.Companion.createKCallableTypeForReference(
                    resolvedCall.getResultingDescriptor(), lhs, reflectionTypes, context.scope.getOwnerDescriptor()
            );
        }

        if (expectedTypeIsUnknown) {
            return functionPlaceholders.createFunctionPlaceholderType(Collections.<KotlinType>emptyList(), false);
        }

        return FunctionTypeResolveUtilsKt.createFunctionType(
                builtIns, Annotations.Companion.getEMPTY(), null, Collections.<KotlinType>emptyList(), null, TypeUtils.DONT_CARE
        );
    }

    @NotNull
    public KotlinTypeInfo getFunctionLiteralTypeInfo(
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
                   : FunctionTypeResolveUtilsKt.createFunctionType(
                           builtIns, Annotations.Companion.getEMPTY(), null, Collections.<KotlinType>emptyList(), null, DONT_CARE
                   );
        }
        List<KtParameter> valueParameters = function.getValueParameters();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<KotlinType> parameterTypes = new ArrayList<KotlinType>(valueParameters.size());
        List<Name> parameterNames = new ArrayList<Name>(valueParameters.size());
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

        return expectedTypeIsUnknown && isFunctionLiteral
               ? functionPlaceholders.createFunctionPlaceholderType(parameterTypes, /* hasDeclaredArguments = */ true)
               : FunctionTypeResolveUtilsKt.createFunctionType(
                       builtIns, Annotations.Companion.getEMPTY(), receiverType, parameterTypes, parameterNames, returnType
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

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            // Here we go inside arguments and determine additional data flow information for them
            KotlinTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, resolveArgumentsMode);
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
