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

package org.jetbrains.kotlin.types.expressions

import com.google.common.collect.Lists
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.AUTO_CREATED_IT
import org.jetbrains.kotlin.resolve.BindingContext.EXPECTED_RETURN_TYPE
import org.jetbrains.kotlin.resolve.BindingContext.EXPRESSION_TYPE
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.CANT_INFER_LAMBDA_PARAM_TYPE
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.CoercionStrategy.COERCION_TO_UNIT
import org.jetbrains.kotlin.utils.addIfNotNull

public class FunctionsTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitNamedFunction(function: JetNamedFunction, data: ExpressionTypingContext): JetTypeInfo {
        return visitNamedFunction(function, data, false, null)
    }

    public fun visitNamedFunction(
            function: JetNamedFunction,
            context: ExpressionTypingContext,
            isStatement: Boolean,
            statementScope: WritableScope? // must be not null if isStatement
    ): JetTypeInfo {
        if (!isStatement) {
            // function expression
            if (!function.getTypeParameters().isEmpty()) {
                context.trace.report(TYPE_PARAMETERS_NOT_ALLOWED.on(function))
            }
            for (parameter in function.getValueParameters()) {
                if (parameter.hasDefaultValue()) {
                    context.trace.report(FUNCTION_EXPRESSION_PARAMETER_WITH_DEFAULT_VALUE.on(parameter))
                }
                if (parameter.isVarArg()) {
                    context.trace.report(USELESS_VARARG_ON_PARAMETER.on(parameter))
                }
            }
        }

        val services = components.expressionTypingServices

        val functionDescriptor: SimpleFunctionDescriptor
        if (isStatement) {
            functionDescriptor = services.getDescriptorResolver().resolveFunctionDescriptorWithAnnotationArguments(
                    context.scope.getContainingDeclaration(), context.scope, function, context.trace, context.dataFlowInfo)
            assert(statementScope != null) {
                "statementScope must be not null for function: " + function.getName() + " at location " + DiagnosticUtils.atLocation(function)
            }
            statementScope!!.addFunctionDescriptor(functionDescriptor)
        }
        else {
            functionDescriptor = services.getDescriptorResolver().resolveFunctionExpressionDescriptor(
                    context.scope.getContainingDeclaration(), context.scope, function, context.trace, context.dataFlowInfo)
        }

        val functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace)
        services.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace)

        services.resolveValueParameters(function.getValueParameters(), functionDescriptor.getValueParameters(), context.scope,
                                        context.dataFlowInfo, context.trace);

        ModifiersChecker.create(context.trace, components.additionalCheckerProvider)
                .checkModifiersForLocalDeclaration(function, functionDescriptor)
        if (!function.hasBody()) {
            context.trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor))
        }

        if (isStatement) {
            return DataFlowUtils.checkStatementType(function, context as ResolutionContext<*>, context.dataFlowInfo)
        }
        else {
            return DataFlowUtils.checkType(createFunctionType(functionDescriptor), function, context as ResolutionContext<*>, context.dataFlowInfo)
        }
    }

    private fun createFunctionType(functionDescriptor: SimpleFunctionDescriptor): JetType? {
        val receiverType = functionDescriptor.getExtensionReceiverParameter()?.getType()

        val returnType = functionDescriptor.getReturnType()
        if (returnType == null) {
            return null
        }

        val parameters = functionDescriptor.getValueParameters().map {
            it.getType()
        }

        return components.builtIns.getFunctionType(Annotations.EMPTY, receiverType, parameters, returnType)
    }

    override fun visitFunctionLiteralExpression(expression: JetFunctionLiteralExpression, context: ExpressionTypingContext): JetTypeInfo? {
        if (!expression.getFunctionLiteral().hasBody()) return null

        val expectedType = context.expectedType
        val functionTypeExpected = !noExpectedType(expectedType) && KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)

        val functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected)
        val safeReturnType = computeReturnType(expression, context, functionDescriptor, functionTypeExpected)
        functionDescriptor.setReturnType(safeReturnType)

        val receiver = DescriptorUtils.getReceiverParameterType(functionDescriptor.getExtensionReceiverParameter())
        val valueParametersTypes = ExpressionTypingUtils.getValueParametersTypes(functionDescriptor.getValueParameters())
        val resultType = components.builtIns.getFunctionType(Annotations.EMPTY, receiver, valueParametersTypes, safeReturnType)
        if (!noExpectedType(expectedType) && KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)) {
            // all checks were done before
            return JetTypeInfo.create(resultType, context.dataFlowInfo)
        }

        return DataFlowUtils.checkType(resultType, expression, context as ResolutionContext<*>, context.dataFlowInfo)
    }

    private fun createFunctionDescriptor(
            expression: JetFunctionLiteralExpression,
            context: ExpressionTypingContext,
            functionTypeExpected: Boolean
    ): AnonymousFunctionDescriptor {
        val functionLiteral = expression.getFunctionLiteral()
        val receiverTypeRef = functionLiteral.getReceiverTypeReference()
        val functionDescriptor = AnonymousFunctionDescriptor(context.scope.getContainingDeclaration(), Annotations.EMPTY,
                                                             CallableMemberDescriptor.Kind.DECLARATION, functionLiteral.toSourceElement())

        val valueParameterDescriptors = createValueParameterDescriptors(context, functionLiteral, functionDescriptor, functionTypeExpected)

        val effectiveReceiverType: JetType?
        if (receiverTypeRef == null) {
            if (functionTypeExpected) {
                effectiveReceiverType = KotlinBuiltIns.getReceiverType(context.expectedType)
            }
            else {
                effectiveReceiverType = null
            }
        }
        else {
            effectiveReceiverType = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, receiverTypeRef,
                                                                                                      context.trace, true)
        }
        functionDescriptor.initialize(effectiveReceiverType, ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER, listOf(),
                                      valueParameterDescriptors, /*unsubstitutedReturnType = */ null, Modality.FINAL, Visibilities.LOCAL)
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, functionLiteral, functionDescriptor)
        return functionDescriptor
    }

    private fun createValueParameterDescriptors(
            context: ExpressionTypingContext,
            functionLiteral: JetFunctionLiteral,
            functionDescriptor: FunctionDescriptorImpl,
            functionTypeExpected: Boolean
    ): List<ValueParameterDescriptor> {
        val valueParameterDescriptors = Lists.newArrayList<ValueParameterDescriptor>()
        val declaredValueParameters = functionLiteral.getValueParameters()

        val expectedValueParameters = if (functionTypeExpected) KotlinBuiltIns.getValueParameters(functionDescriptor, context.expectedType)
        else null

        val valueParameterList = functionLiteral.getValueParameterList()
        val hasDeclaredValueParameters = valueParameterList != null
        if (functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters!!.size() == 1) {
            val valueParameterDescriptor = expectedValueParameters!!.get(0)
            val it = ValueParameterDescriptorImpl(functionDescriptor, null, 0, Annotations.EMPTY, Name.identifier("it"),
                                                  valueParameterDescriptor.getType(), valueParameterDescriptor.hasDefaultValue(),
                                                  valueParameterDescriptor.getVarargElementType(), SourceElement.NO_SOURCE)
            valueParameterDescriptors.add(it)
            context.trace.record<ValueParameterDescriptor>(AUTO_CREATED_IT, it)
        }
        else {
            if (expectedValueParameters != null && declaredValueParameters.size() != expectedValueParameters.size()) {
                val expectedParameterTypes = ExpressionTypingUtils.getValueParametersTypes(expectedValueParameters)
                context.trace.report(EXPECTED_PARAMETERS_NUMBER_MISMATCH.on(functionLiteral, expectedParameterTypes.size(), expectedParameterTypes))
            }
            for (i in declaredValueParameters.indices) {
                val valueParameterDescriptor = createValueParameterDescriptor(context, functionDescriptor, declaredValueParameters, expectedValueParameters, i)
                valueParameterDescriptors.add(valueParameterDescriptor)
            }
        }
        return valueParameterDescriptors
    }

    private fun createValueParameterDescriptor(
            context: ExpressionTypingContext,
            functionDescriptor: FunctionDescriptorImpl,
            declaredValueParameters: List<JetParameter>,
            expectedValueParameters: List<ValueParameterDescriptor>?,
            index: Int
    ): ValueParameterDescriptor {
        val declaredParameter = declaredValueParameters.get(index)
        val typeReference = declaredParameter.getTypeReference()

        val expectedType: JetType?
        if (expectedValueParameters != null && index < expectedValueParameters.size()) {
            expectedType = expectedValueParameters.get(index).getType()
        }
        else {
            expectedType = null
        }
        val type: JetType
        if (typeReference != null) {
            type = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true)
            if (expectedType != null) {
                if (!JetTypeChecker.DEFAULT.isSubtypeOf(expectedType, type)) {
                    context.trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(declaredParameter, expectedType))
                }
            }
        }
        else {
            val containsUninferredParameter = TypeUtils.containsSpecialType(expectedType) {
                    TypeUtils.isDontCarePlaceholder(it) || ErrorUtils.isUninferredParameter(it)
                }
            if (expectedType == null || containsUninferredParameter) {
                context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter))
            }
            if (expectedType != null) {
                type = expectedType
            }
            else {
                type = CANT_INFER_LAMBDA_PARAM_TYPE
            }
        }
        return components.expressionTypingServices.getDescriptorResolver()
                .resolveValueParameterDescriptorWithAnnotationArguments(context.scope, functionDescriptor, declaredParameter,
                                                                        index, type, context.trace)
    }

    private fun computeReturnType(
            expression: JetFunctionLiteralExpression,
            context: ExpressionTypingContext,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            functionTypeExpected: Boolean
    ): JetType {
        val expectedReturnType = if (functionTypeExpected) KotlinBuiltIns.getReturnTypeFromFunctionType(context.expectedType) else null
        val returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType);

        if (!expression.getFunctionLiteral().hasDeclaredReturnType() && functionTypeExpected) {
            if (KotlinBuiltIns.isUnit(expectedReturnType!!)) {
                return components.builtIns.getUnitType()
            }
        }
        return returnType ?: CANT_INFER_LAMBDA_PARAM_TYPE
    }


    private fun computeUnsafeReturnType(
            expression: JetFunctionLiteralExpression,
            context: ExpressionTypingContext,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            expectedReturnType: JetType?
    ): JetType? {
        val functionLiteral = expression.getFunctionLiteral()
        val declaredReturnType = functionLiteral.getTypeReference()?.let {
            val type = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, it, context.trace, true)
            if (expectedReturnType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(type, expectedReturnType)) {
                context.trace.report(EXPECTED_RETURN_TYPE_MISMATCH.on(it, expectedReturnType))
            }
            type
        }

        val expectedType = declaredReturnType ?: (expectedReturnType ?: NO_EXPECTED_TYPE)
        val functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace)
        val newContext = context.replaceScope(functionInnerScope).replaceExpectedType(expectedType)

        // This is needed for ControlStructureTypingVisitor#visitReturnExpression() to properly type-check returned expressions
        context.trace.record(EXPECTED_RETURN_TYPE, functionLiteral, expectedType)
        val typeOfBodyExpression = // Type-check the body
                components.expressionTypingServices.getBlockReturnedType(functionLiteral.getBodyExpression(), COERCION_TO_UNIT, newContext).getType()

        return declaredReturnType ?: computeReturnTypeBasedOnReturnExpressions(functionLiteral, context, typeOfBodyExpression)
    }

    private fun computeReturnTypeBasedOnReturnExpressions(
            functionLiteral: JetFunctionLiteral,
            context: ExpressionTypingContext,
            typeOfBodyExpression: JetType?
    ): JetType? {
        val returnedExpressionTypes = Lists.newArrayList<JetType>()

        var hasEmptyReturn = false
        val returnExpressions = collectReturns(functionLiteral, context.trace)
        for (returnExpression in returnExpressions) {
            val returnedExpression = returnExpression.getReturnedExpression()
            if (returnedExpression == null) {
                hasEmptyReturn = true
            }
            else {
                // the type should have been computed by getBlockReturnedType() above, but can be null, if returnExpression contains some error
                returnedExpressionTypes.addIfNotNull(context.trace.get<JetExpression, JetType>(EXPRESSION_TYPE, returnedExpression))
            }
        }

        if (hasEmptyReturn) {
            for (returnExpression in returnExpressions) {
                val returnedExpression = returnExpression.getReturnedExpression()
                if (returnedExpression != null) {
                    val type = context.trace.get<JetExpression, JetType>(EXPRESSION_TYPE, returnedExpression)
                    if (type == null || !KotlinBuiltIns.isUnit(type)) {
                        context.trace.report(RETURN_TYPE_MISMATCH.on(returnedExpression, components.builtIns.getUnitType()))
                    }
                }
            }
            return components.builtIns.getUnitType()
        }
        returnedExpressionTypes.addIfNotNull(typeOfBodyExpression)

        if (returnedExpressionTypes.isEmpty()) return null
        return CommonSupertypes.commonSupertype(returnedExpressionTypes)
    }

    private fun collectReturns(functionLiteral: JetFunctionLiteral, trace: BindingTrace): Collection<JetReturnExpression> {
        val result = Lists.newArrayList<JetReturnExpression>()
        val bodyExpression = functionLiteral.getBodyExpression()
        bodyExpression?.accept(object : JetTreeVisitor<MutableList<JetReturnExpression>>() {
            override fun visitReturnExpression(expression: JetReturnExpression, data: MutableList<JetReturnExpression>): Void? {
                data.add(expression)
                return null
            }
        }, result)
        return result.filter {
            // No label => non-local return
            // Either a local return of inner lambda/function or a non-local return
            it.getTargetLabel()?.let { trace.get(BindingContext.LABEL_TARGET, it) } == functionLiteral
        }
    }
}
