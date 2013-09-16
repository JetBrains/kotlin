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
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.constants.ErrorValue;
import org.jetbrains.jet.lang.resolve.constants.NumberValueTypeConstructor;
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
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.getRecordedTypeInfo;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.*;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SKIP_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

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
            return isFunctionOrErrorType(expectedType) || KotlinBuiltIns.getInstance().isAny(expectedType); //todo function type extends
        }
        return JetTypeChecker.INSTANCE.isSubtypeOf(actualType, expectedType);
    }

    private static boolean isFunctionOrErrorType(@NotNull JetType supertype) {
        return KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(supertype) || supertype.isError();
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context) {
        checkTypesWithNoCallee(context, SKIP_FUNCTION_ARGUMENTS);
    }

    public void checkTypesWithNoCallee(@NotNull CallResolutionContext<?> context, @NotNull ResolveArgumentsMode resolveFunctionArgumentBodies) {
        if (context.checkArguments == CheckValueArgumentsMode.DISABLED) return;

        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof JetFunctionLiteralExpression)) {
                checkArgumentType(context, argumentExpression);
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
                checkArgumentType(context, argumentExpression);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            checkArgumentType(context, expression);
        }
    }

    public void checkUnmappedArgumentTypes(CallResolutionContext<?> context, Set<ValueArgument> unmappedArguments) {
        for (ValueArgument valueArgument : unmappedArguments) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                checkArgumentType(context, argumentExpression);
            }
        }
    }

    private void checkArgumentType(CallResolutionContext<?> context, JetExpression argumentExpression) {
        expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
        updateResultArgumentTypeIfNotDenotable(context, argumentExpression);
    }

    public <D extends CallableDescriptor> void checkTypesForFunctionArguments(CallResolutionContext<?> context, ResolvedCallImpl<D> resolvedCall) {
        Map<ValueParameterDescriptor, ResolvedValueArgument> arguments = resolvedCall.getValueArguments();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : arguments.entrySet()) {
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();
            JetType varargElementType = valueParameterDescriptor.getVarargElementType();
            JetType functionType;
            if (varargElementType != null) {
                functionType = varargElementType;
            }
            else {
                functionType = valueParameterDescriptor.getType();
            }
            ResolvedValueArgument valueArgument = entry.getValue();
            List<ValueArgument> valueArguments = valueArgument.getArguments();
            for (ValueArgument argument : valueArguments) {
                JetExpression expression = argument.getArgumentExpression();
                if (expression instanceof JetFunctionLiteralExpression) {
                    expressionTypingServices.getType(context.scope, expression, functionType, context.dataFlowInfo, context.trace);
                }
            }
        }
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
        JetExpression deparenthesizedExpression = JetPsiUtil.deparenthesize(JetPsiUtil.unwrapFromBlock(expression), false);
        if (deparenthesizedExpression instanceof JetFunctionLiteralExpression) {
            return getFunctionLiteralTypeInfo(expression, (JetFunctionLiteralExpression) deparenthesizedExpression, context, resolveArgumentsMode);
        }
        JetTypeInfo recordedTypeInfo = getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }
        ResolutionContext newContext = context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.DEPENDENT).replaceExpressionPosition(ExpressionPosition.FREE);

        return expressionTypingServices.getTypeInfo(expression, newContext);
    }

    @NotNull
    public JetTypeInfo getFunctionLiteralTypeInfo(
            @NotNull JetExpression expression,
            @NotNull JetFunctionLiteralExpression functionLiteralExpression,
            @NotNull CallResolutionContext<?> context,
            @NotNull ResolveArgumentsMode resolveArgumentsMode
    ) {
        if (resolveArgumentsMode == SKIP_FUNCTION_ARGUMENTS) {
            JetType type = getFunctionLiteralType(functionLiteralExpression, context.scope, context.trace);
            return JetTypeInfo.create(type, context.dataFlowInfo);
        }
        return expressionTypingServices.getTypeInfo(expression, context);
    }

    @Nullable
    public JetType getFunctionLiteralType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace
    ) {
        if (expression.getFunctionLiteral().getValueParameterList() == null) {
            return PLACEHOLDER_FUNCTION_TYPE;
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
        return KotlinBuiltIns.getInstance().getFunctionType(Collections.<AnnotationDescriptor>emptyList(), receiverType, parameterTypes,
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
            JetTypeInfo typeInfoForCall = getArgumentTypeInfo(expression, newContext, SKIP_FUNCTION_ARGUMENTS);
            infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());
        }
    }

    @Nullable
    public <D extends CallableDescriptor> JetType updateResultArgumentTypeIfNotDenotable(
            @NotNull ResolutionContext context,
            @NotNull JetExpression expression
    ) {
        JetType type = context.trace.get(BindingContext.EXPRESSION_TYPE, expression);
        if (type != null && !type.getConstructor().isDenotable()) {
            if (type.getConstructor() instanceof NumberValueTypeConstructor) {
                NumberValueTypeConstructor constructor = (NumberValueTypeConstructor) type.getConstructor();
                JetType primitiveType = TypeUtils.getPrimitiveNumberType(constructor, context.expectedType);
                updateNumberType(primitiveType, expression, context);
                return primitiveType;
            }
        }
        return type;
    }

    private <D extends CallableDescriptor> void updateNumberType(
            @NotNull JetType numberType,
            @Nullable JetExpression expression,
            @NotNull ResolutionContext context
    ) {
        if (expression == null) return;
        BindingContextUtils.updateRecordedType(numberType, expression, context.trace, false);

        if (!(expression instanceof JetConstantExpression)) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression, false);
            if (deparenthesized != expression) {
                updateNumberType(numberType, deparenthesized, context);
            }
            if (deparenthesized instanceof JetBlockExpression) {
                JetElement lastStatement = JetPsiUtil.getLastStatementInABlock((JetBlockExpression) deparenthesized);
                if (lastStatement instanceof JetExpression) {
                    updateNumberType(numberType, (JetExpression) lastStatement, context);
                }
            }
            return;
        }
        CompileTimeConstant<?> constant =
                new CompileTimeConstantResolver().getCompileTimeConstant((JetConstantExpression) expression, numberType);

        if (!(constant instanceof ErrorValue)) {
            context.trace.record(BindingContext.COMPILE_TIME_VALUE, expression, constant);
        }
    }
}
