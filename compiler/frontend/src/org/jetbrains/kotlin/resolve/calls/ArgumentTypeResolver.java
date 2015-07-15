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
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.JetTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.BindingContextUtils.getRecordedTypeInfo;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.inference.InferencePackage.createTypeForFunctionPlaceholder;
import static org.jetbrains.kotlin.types.TypeUtils.DONT_CARE;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {

    private TypeResolver typeResolver;
    private ExpressionTypingServices expressionTypingServices;
    private KotlinBuiltIns builtIns;

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    public static boolean isSubtypeOfForArgumentType(
            @NotNull JetType actualType,
            @NotNull JetType expectedType
    ) {
        if (ErrorUtils.isFunctionPlaceholder(actualType)) {
            JetType functionType = createTypeForFunctionPlaceholder(actualType, expectedType);
            return JetTypeChecker.DEFAULT.isSubtypeOf(functionType, expectedType);
        }
        return JetTypeChecker.DEFAULT.isSubtypeOf(actualType, expectedType);
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context) {
        checkTypesWithNoCallee(context, SHAPE_FUNCTION_ARGUMENTS);
    }

    public void checkTypesWithNoCallee(
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveFunctionArgumentBodies
    ) {
        if (context.checkArguments == CheckValueArgumentsMode.DISABLED) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof JetFunctionLiteralExpression)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }

        if (resolveFunctionArgumentBodies == RESOLVE_FUNCTION_ARGUMENTS) {
            checkTypesForFunctionArgumentsWithNoCallee(context);
        }

        for (JetTypeProjection typeProjection : context.call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    public void checkTypesForFunctionArgumentsWithNoCallee(@NotNull CallResolutionContext<?> context) {
        if (context.checkArguments == CheckValueArgumentsMode.DISABLED) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && isFunctionLiteralArgument(argumentExpression, context)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }
    }

    private void checkArgumentTypeWithNoCallee(CallResolutionContext<?> context, JetExpression argumentExpression) {
        expressionTypingServices.getTypeInfo(argumentExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression);
    }

    public static boolean isFunctionLiteralArgument(
            @NotNull JetExpression expression, @NotNull ResolutionContext context
    ) {
        return getFunctionLiteralArgumentIfAny(expression, context) != null;
    }

    @NotNull
    public static JetFunction getFunctionLiteralArgument(
            @NotNull JetExpression expression, @NotNull ResolutionContext context
    ) {
        assert isFunctionLiteralArgument(expression, context);
        //noinspection ConstantConditions
        return getFunctionLiteralArgumentIfAny(expression, context);
    }

    @Nullable
    private static JetFunction getFunctionLiteralArgumentIfAny(
            @NotNull JetExpression expression, @NotNull ResolutionContext context
    ) {
        JetExpression deparenthesizedExpression = getLastElementDeparenthesized(expression, context);
        if (deparenthesizedExpression instanceof JetFunctionLiteralExpression) {
            return ((JetFunctionLiteralExpression) deparenthesizedExpression).getFunctionLiteral();
        }
        if (deparenthesizedExpression instanceof JetFunction) {
            return (JetFunction) deparenthesizedExpression;
        }
        return null;
    }

    @Nullable
    public static JetExpression getLastElementDeparenthesized(
            @Nullable JetExpression expression,
            @NotNull ResolutionContext context
    ) {
        JetExpression deparenthesizedExpression = JetPsiUtil.deparenthesize(expression, false);
        if (deparenthesizedExpression instanceof JetBlockExpression) {
            JetBlockExpression blockExpression = (JetBlockExpression) deparenthesizedExpression;
            // todo
            // This case is a temporary hack for 'if' branches.
            // The right way to implement this logic is to interpret 'if' branches as function literals with explicitly-typed signatures
            // (no arguments and no receiver) and therefore analyze them straight away (not in the 'complete' phase).
            JetExpression lastStatementInABlock = ResolvePackage.getLastStatementInABlock(context.statementFilter, blockExpression);
            if (lastStatementInABlock != null) {
                return getLastElementDeparenthesized(lastStatementInABlock, context);
            }
        }
        return deparenthesizedExpression;
    }

    @NotNull
    public JetTypeInfo getArgumentTypeInfo(
            @Nullable JetExpression expression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (expression == null) {
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }
        if (isFunctionLiteralArgument(expression, context)) {
            return getFunctionLiteralTypeInfo(expression, getFunctionLiteralArgument(expression, context), context, resolveArgumentsMode);
        }
        JetTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }
        ResolutionContext newContext = context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(DEPENDENT);

        return expressionTypingServices.getTypeInfo(expression, newContext);
    }

    @NotNull
    public JetTypeInfo getFunctionLiteralTypeInfo(
            @NotNull JetExpression expression,
            @NotNull JetFunction functionLiteral,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
            JetType type = getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, true);
            return TypeInfoFactoryPackage.createTypeInfo(type, context);
        }
        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(INDEPENDENT));
    }

    @Nullable
    public JetType getShapeTypeOfFunctionLiteral(
            @NotNull JetFunction function,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            boolean expectedTypeIsUnknown
    ) {
        boolean isFunctionLiteral = function instanceof JetFunctionLiteral;
        if (function.getValueParameterList() == null && isFunctionLiteral) {
            return expectedTypeIsUnknown
                   ? ErrorUtils.createFunctionPlaceholderType(Collections.<JetType>emptyList(), /* hasDeclaredArguments = */ false)
                   : builtIns.getFunctionType(Annotations.EMPTY, null, Collections.<JetType>emptyList(), DONT_CARE);
        }
        List<JetParameter> valueParameters = function.getValueParameters();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<JetType> parameterTypes = Lists.newArrayList();
        for (JetParameter parameter : valueParameters) {
            parameterTypes.add(resolveTypeRefWithDefault(parameter.getTypeReference(), scope, temporaryTrace, DONT_CARE));
        }
        JetType returnType = resolveTypeRefWithDefault(function.getTypeReference(), scope, temporaryTrace, DONT_CARE);
        assert returnType != null;
        JetType receiverType = resolveTypeRefWithDefault(function.getReceiverTypeReference(), scope, temporaryTrace, null);

        return expectedTypeIsUnknown && isFunctionLiteral
               ? ErrorUtils.createFunctionPlaceholderType(parameterTypes, /* hasDeclaredArguments = */ true)
               : builtIns.getFunctionType(Annotations.EMPTY, receiverType, parameterTypes, returnType);
    }

    @Nullable
    public JetType resolveTypeRefWithDefault(
            @Nullable JetTypeReference returnTypeRef,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            @Nullable JetType defaultValue
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
            if (CallUtilPackage.isSafeCall(call)) {
                initialDataFlowInfo = initialDataFlowInfo.disequate(receiverDataFlowValue, DataFlowValue.NULL);
            }
        }
        infoForArguments.setInitialDataFlowInfo(initialDataFlowInfo);

        for (ValueArgument argument : call.getValueArguments()) {
            JetExpression expression = argument.getArgumentExpression();
            if (expression == null) continue;

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            // Here we go inside arguments and determine additional data flow information for them
            JetTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, SHAPE_FUNCTION_ARGUMENTS);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
        }
    }

    @Nullable
    public static JetType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull JetExpression expression
    ) {
        JetType type = context.trace.getType(expression);
        if (type != null && !type.getConstructor().isDenotable()) {
            if (type.getConstructor() instanceof IntegerValueTypeConstructor) {
                IntegerValueTypeConstructor constructor = (IntegerValueTypeConstructor) type.getConstructor();
                JetType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, context.expectedType);
                updateNumberType(primitiveType, expression, context);
                return primitiveType;
            }
        }
        return type;
    }

    public static void updateNumberType(
            @NotNull JetType numberType,
            @Nullable JetExpression expression,
            @NotNull ResolutionContext context
    ) {
        if (expression == null) return;
        BindingContextUtils.updateRecordedType(numberType, expression, context.trace, false);

        if (!(expression instanceof JetConstantExpression)) {
            JetExpression deparenthesized = getLastElementDeparenthesized(expression, context);
            if (deparenthesized != expression) {
                updateNumberType(numberType, deparenthesized, context);
            }
            return;
        }

        ConstantExpressionEvaluator.evaluate(expression, context.trace, numberType);
    }
}
