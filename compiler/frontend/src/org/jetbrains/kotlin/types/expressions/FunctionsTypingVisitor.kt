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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.checkReservedPrefixWord
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.EXPECTED_RETURN_TYPE
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.*
import org.jetbrains.kotlin.types.expressions.CoercionStrategy.COERCION_TO_UNIT
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.utils.addIfNotNull

internal class FunctionsTypingVisitor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitNamedFunction(function: KtNamedFunction, data: ExpressionTypingContext): KotlinTypeInfo {
        return visitNamedFunction(function, data, false, null)
    }

    fun visitNamedFunction(
            function: KtNamedFunction,
            context: ExpressionTypingContext,
            isStatement: Boolean,
            statementScope: LexicalWritableScope? // must be not null if isStatement
    ): KotlinTypeInfo {
        checkReservedAsync(context, function)
        if (!isStatement) {
            // function expression
            if (!function.getTypeParameters().isEmpty()) {
                context.trace.report(TYPE_PARAMETERS_NOT_ALLOWED.on(function))
            }

            if (function.getName() != null) {
                context.trace.report(ANONYMOUS_FUNCTION_WITH_NAME.on(function.nameIdentifier!!))
            }

            for (parameter in function.getValueParameters()) {
                if (parameter.hasDefaultValue()) {
                    context.trace.report(ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE.on(parameter))
                }
                if (parameter.isVarArg()) {
                    context.trace.report(USELESS_VARARG_ON_PARAMETER.on(parameter))
                }
            }
        }

        val functionDescriptor: SimpleFunctionDescriptor
        if (isStatement) {
            functionDescriptor = components.functionDescriptorResolver.resolveFunctionDescriptor(
                    context.scope.ownerDescriptor, context.scope, function, context.trace, context.dataFlowInfo)
            assert(statementScope != null) {
                "statementScope must be not null for function: " + function.getName() + " at location " + DiagnosticUtils.atLocation(function)
            }
            statementScope!!.addFunctionDescriptor(functionDescriptor)
        }
        else {
            functionDescriptor = components.functionDescriptorResolver.resolveFunctionExpressionDescriptor(
                    context.scope.ownerDescriptor, context.scope, function,
                    context.trace, context.dataFlowInfo, context.expectedType
            )
        }
        // Necessary for local functions
        ForceResolveUtil.forceResolveAllContents(functionDescriptor.annotations);

        val functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace)
        components.expressionTypingServices.checkFunctionReturnType(
                functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace
        )

        components.valueParameterResolver.resolveValueParameters(
                function.getValueParameters(), functionDescriptor.valueParameters, context.scope, context.dataFlowInfo, context.trace
        )

        function.checkTypeReferences(context.trace)
        components.modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(function, functionDescriptor)
        components.identifierChecker.checkDeclaration(function, context.trace)
        components.declarationsCheckerBuilder.withTrace(context.trace).checkFunction(function, functionDescriptor)

        if (isStatement) {
            return createTypeInfo(components.dataFlowAnalyzer.checkStatementType(function, context), context)
        }
        else {
            return components.dataFlowAnalyzer.createCheckedTypeInfo(createFunctionType(functionDescriptor), context, function)
        }
    }

    private fun createFunctionType(functionDescriptor: SimpleFunctionDescriptor): KotlinType? {
        val receiverType = functionDescriptor.extensionReceiverParameter?.type

        val returnType = functionDescriptor.returnType
        if (returnType == null) {
            return null
        }

        val parameters = functionDescriptor.valueParameters.map {
            it.type
        }

        return components.builtIns.getFunctionType(Annotations.EMPTY, receiverType, parameters, returnType)
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, context: ExpressionTypingContext): KotlinTypeInfo? {
        checkReservedAsync(context, expression)

        if (!expression.functionLiteral.hasBody()) return null

        val expectedType = context.expectedType
        val functionTypeExpected = !noExpectedType(expectedType) && KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)

        val functionDescriptor = createFunctionLiteralDescriptor(expression, context)
        expression.valueParameters.forEach {
            components.identifierChecker.checkDeclaration(it, context.trace)
            UnderscoreChecker.checkNamed(it, context.trace)
        }
        val safeReturnType = computeReturnType(expression, context, functionDescriptor, functionTypeExpected)
        functionDescriptor.setReturnType(safeReturnType)

        val resultType = createFunctionType(functionDescriptor)!!
        if (functionTypeExpected) {
            // all checks were done before
            return createTypeInfo(resultType, context)
        }

        return components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, expression)
    }

    private fun checkReservedAsync(context: ExpressionTypingContext, expression: PsiElement) {
        checkReservedPrefixWord(context.trace, expression, "async", KtTokens.BINARY_OPERATIONS, "async block/lambda. Use 'async() { ... }' or 'async(fun...)'")
    }

    private fun createFunctionLiteralDescriptor(
            expression: KtLambdaExpression,
            context: ExpressionTypingContext
    ): AnonymousFunctionDescriptor {
        val functionLiteral = expression.functionLiteral
        val functionDescriptor = AnonymousFunctionDescriptor(
            context.scope.ownerDescriptor,
            components.annotationResolver.resolveAnnotationsWithArguments(context.scope, expression.getAnnotationEntries(), context.trace),
            CallableMemberDescriptor.Kind.DECLARATION, functionLiteral.toSourceElement()
        )
        components.functionDescriptorResolver.
                initializeFunctionDescriptorAndExplicitReturnType(context.scope.ownerDescriptor, context.scope, functionLiteral,
                                                                  functionDescriptor, context.trace, context.expectedType)
        for (parameterDescriptor in functionDescriptor.valueParameters) {
            ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
        }
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, functionLiteral, functionDescriptor)
        return functionDescriptor
    }

    private fun computeReturnType(
            expression: KtLambdaExpression,
            context: ExpressionTypingContext,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            functionTypeExpected: Boolean
    ): KotlinType {
        val expectedReturnType = if (functionTypeExpected) KotlinBuiltIns.getReturnTypeFromFunctionType(context.expectedType) else null
        val returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType);

        if (!expression.functionLiteral.hasDeclaredReturnType() && functionTypeExpected) {
            if (!TypeUtils.noExpectedType(expectedReturnType!!) && KotlinBuiltIns.isUnit(expectedReturnType)) {
                return components.builtIns.unitType
            }
        }
        return returnType ?: CANT_INFER_FUNCTION_PARAM_TYPE
    }

    private fun computeUnsafeReturnType(
            expression: KtLambdaExpression,
            context: ExpressionTypingContext,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            expectedReturnType: KotlinType?
    ): KotlinType? {
        val functionLiteral = expression.functionLiteral

        val expectedType = expectedReturnType ?: NO_EXPECTED_TYPE
        val functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace)
        val newContext = context.replaceScope(functionInnerScope).replaceExpectedType(expectedType)

        // This is needed for ControlStructureTypingVisitor#visitReturnExpression() to properly type-check returned expressions
        context.trace.record(EXPECTED_RETURN_TYPE, functionLiteral, expectedType)
        val typeOfBodyExpression = // Type-check the body
                components.expressionTypingServices.getBlockReturnedType(functionLiteral.bodyExpression!!, COERCION_TO_UNIT, newContext).type

        return computeReturnTypeBasedOnReturnExpressions(functionLiteral, context, typeOfBodyExpression)
    }

    private fun computeReturnTypeBasedOnReturnExpressions(
            functionLiteral: KtFunctionLiteral,
            context: ExpressionTypingContext,
            typeOfBodyExpression: KotlinType?
    ): KotlinType? {
        val returnedExpressionTypes = Lists.newArrayList<KotlinType>()

        var hasEmptyReturn = false
        val returnExpressions = collectReturns(functionLiteral, context.trace)
        for (returnExpression in returnExpressions) {
            val returnedExpression = returnExpression.returnedExpression
            if (returnedExpression == null) {
                hasEmptyReturn = true
            }
            else {
                // the type should have been computed by getBlockReturnedType() above, but can be null, if returnExpression contains some error
                returnedExpressionTypes.addIfNotNull(context.trace.getType(returnedExpression))
            }
        }

        if (hasEmptyReturn) {
            for (returnExpression in returnExpressions) {
                val returnedExpression = returnExpression.returnedExpression
                if (returnedExpression != null) {
                    val type = context.trace.getType(returnedExpression)
                    if (type == null || !KotlinBuiltIns.isUnit(type)) {
                        context.trace.report(RETURN_TYPE_MISMATCH.on(returnedExpression, components.builtIns.getUnitType()))
                    }
                }
            }
            return components.builtIns.unitType
        }
        returnedExpressionTypes.addIfNotNull(typeOfBodyExpression)

        if (returnedExpressionTypes.isEmpty()) return null
        return CommonSupertypes.commonSupertype(returnedExpressionTypes)
    }

    private fun collectReturns(functionLiteral: KtFunctionLiteral, trace: BindingTrace): Collection<KtReturnExpression> {
        val result = Lists.newArrayList<KtReturnExpression>()
        val bodyExpression = functionLiteral.bodyExpression
        bodyExpression?.accept(object : KtTreeVisitor<MutableList<KtReturnExpression>>() {
            override fun visitReturnExpression(expression: KtReturnExpression, data: MutableList<KtReturnExpression>): Void? {
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
