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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.getRecordedTypeInfo;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.DEPENDENT;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.types.TypeUtils.*;

public class ArgumentTypeResolver {

    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    public static boolean isSubtypeOfForArgumentType(
            @NotNull JetType actualType,
            @NotNull JetType expectedType
    ) {
        if (actualType == PLACEHOLDER_FUNCTION_TYPE) {
            return isFunctionOrErrorType(expectedType) || KotlinBuiltIns.getInstance().isAnyOrNullableAny(expectedType); //todo function type extends
        }
        return JetTypeChecker.DEFAULT.isSubtypeOf(actualType, expectedType);
    }

    private static boolean isFunctionOrErrorType(@NotNull JetType supertype) {
        return KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(supertype) || supertype.isError();
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context) {
        checkTypesWithNoCallee(context, SHAPE_FUNCTION_ARGUMENTS);
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context, @NotNull ResolveArgumentsMode resolveFunctionArgumentBodies) {
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
            if (argumentExpression != null && (argumentExpression instanceof JetFunctionLiteralExpression)) {
                checkArgumentTypeWithNoCallee(context, argumentExpression);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            checkArgumentTypeWithNoCallee(context, expression);
        }
    }

    private void checkArgumentTypeWithNoCallee(CallResolutionContext<?> context, JetExpression argumentExpression) {
        expressionTypingServices.getTypeInfo(argumentExpression, context.replaceExpectedType(NO_EXPECTED_TYPE));
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression);
    }

    public static boolean isFunctionLiteralArgument(@NotNull JetExpression expression) {
        return getFunctionLiteralArgumentIfAny(expression) != null;
    }

    @NotNull
    public static JetFunctionLiteralExpression getFunctionLiteralArgument(@NotNull JetExpression expression) {
        assert isFunctionLiteralArgument(expression);
        //noinspection ConstantConditions
        return getFunctionLiteralArgumentIfAny(expression);
    }

    @Nullable
    private static JetFunctionLiteralExpression getFunctionLiteralArgumentIfAny(@NotNull JetExpression expression) {
        JetExpression deparenthesizedExpression = JetPsiUtil.deparenthesize(expression, false);
        if (deparenthesizedExpression instanceof JetBlockExpression) {
            // todo
            // This case is a temporary hack for 'if' branches.
            // The right way to implement this logic is to interpret 'if' branches as function literals with explicitly-typed signatures
            // (no arguments and no receiver) and therefore analyze them straight away (not in the 'complete' phase).
            JetElement lastStatementInABlock = JetPsiUtil.getLastStatementInABlock((JetBlockExpression) deparenthesizedExpression);
            if (lastStatementInABlock instanceof JetExpression) {
                deparenthesizedExpression = JetPsiUtil.deparenthesize((JetExpression) lastStatementInABlock, false);
            }
        }
        if (deparenthesizedExpression instanceof JetFunctionLiteralExpression) {
            return (JetFunctionLiteralExpression) deparenthesizedExpression;
        }
        return null;
    }

    @NotNull
    public JetTypeInfo getArgumentTypeInfo(
            @Nullable JetExpression expression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (expression == null) {
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        if (isFunctionLiteralArgument(expression)) {
            return getFunctionLiteralTypeInfo(expression, getFunctionLiteralArgument(expression), context, resolveArgumentsMode);
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
            @NotNull JetFunctionLiteralExpression functionLiteralExpression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (resolveArgumentsMode == SHAPE_FUNCTION_ARGUMENTS) {
            JetType type = getShapeTypeOfFunctionLiteral(functionLiteralExpression, context.scope, context.trace, true);
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }
        return expressionTypingServices.getTypeInfo(expression, context.replaceContextDependency(INDEPENDENT));
    }

    @Nullable
    public JetType getShapeTypeOfFunctionLiteral(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            boolean expectedTypeIsUnknown
    ) {
        if (expression.getFunctionLiteral().getValueParameterList() == null) {
            return expectedTypeIsUnknown ? PLACEHOLDER_FUNCTION_TYPE : KotlinBuiltIns.getInstance().getFunctionType(
                    Annotations.EMPTY, null, Collections.<JetType>emptyList(), DONT_CARE);
        }
        List<JetParameter> valueParameters = expression.getValueParameters();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<JetType> parameterTypes = Lists.newArrayList();
        for (JetParameter parameter : valueParameters) {
            parameterTypes.add(resolveTypeRefWithDefault(parameter.getTypeReference(), scope, temporaryTrace, DONT_CARE));
        }
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetType returnType = resolveTypeRefWithDefault(functionLiteral.getReturnTypeRef(), scope, temporaryTrace, DONT_CARE);
        assert returnType != null;
        JetType receiverType = resolveTypeRefWithDefault(functionLiteral.getReceiverTypeRef(), scope, temporaryTrace, null);
        return KotlinBuiltIns.getInstance().getFunctionType(Annotations.EMPTY, receiverType, parameterTypes,
                                                            returnType);
    }

    @Nullable
    public JetType resolveTypeRefWithDefault(
            @Nullable JetTypeReference returnTypeRef,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            @Nullable JetType defaultValue
    ) {
        if (returnTypeRef != null) {
            return expressionTypingServices.getTypeResolver().resolveType(scope, returnTypeRef, trace, true);
        }
        return defaultValue;
    }

    public <D extends CallableDescriptor> void analyzeArgumentsAndRecordTypes(
            @NotNull CallResolutionContext<?> context
    ) {
        MutableDataFlowInfoForArguments infoForArguments = context.dataFlowInfoForArguments;
        infoForArguments.setInitialDataFlowInfo(context.dataFlowInfo);

        for (ValueArgument argument : context.call.getValueArguments()) {
            JetExpression expression = argument.getArgumentExpression();
            if (expression == null) continue;

            CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument));
            JetTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, SHAPE_FUNCTION_ARGUMENTS);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
        }
    }

    @Nullable
    public static JetType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull JetExpression expression
    ) {
        JetType type = context.trace.get(BindingContext.EXPRESSION_TYPE, expression);
        if (type != null && !type.getConstructor().isDenotable()) {
            if (type.getConstructor() instanceof IntegerValueTypeConstructor) {
                IntegerValueTypeConstructor constructor = (IntegerValueTypeConstructor) type.getConstructor();
                JetType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, context.expectedType);
                updateNumberType(primitiveType, expression, context.trace);
                return primitiveType;
            }
        }
        return type;
    }

    public static <D extends CallableDescriptor> void updateNumberType(
            @NotNull JetType numberType,
            @Nullable JetExpression expression,
            @NotNull BindingTrace trace
    ) {
        if (expression == null) return;
        BindingContextUtils.updateRecordedType(numberType, expression, trace, false);

        if (!(expression instanceof JetConstantExpression)) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression, false);
            if (deparenthesized != expression) {
                updateNumberType(numberType, deparenthesized, trace);
            }
            if (deparenthesized instanceof JetBlockExpression) {
                JetElement lastStatement = JetPsiUtil.getLastStatementInABlock((JetBlockExpression) deparenthesized);
                if (lastStatement instanceof JetExpression) {
                    updateNumberType(numberType, (JetExpression) lastStatement, trace);
                }
            }
            return;
        }

        ConstantExpressionEvaluator.object$.evaluate(expression, trace, numberType);
    }
}
