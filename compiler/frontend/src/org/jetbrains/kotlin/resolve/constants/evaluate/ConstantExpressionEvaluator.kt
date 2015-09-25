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

package org.jetbrains.kotlin.resolve.constants.evaluate

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.math.BigInteger
import java.util.ArrayList
import java.util.HashMap

public class ConstantExpressionEvaluator(
        internal val builtIns: KotlinBuiltIns
) {
    internal val constantValueFactory = ConstantValueFactory(builtIns)

    public fun updateNumberType(
            numberType: JetType,
            expression: JetExpression?,
            statementFilter: StatementFilter,
            trace: BindingTrace
    ) {
        if (expression == null) return
        BindingContextUtils.updateRecordedType(numberType, expression, trace, false)

        if (expression !is JetConstantExpression) {
            val deparenthesized = JetPsiUtil.getLastElementDeparenthesized(expression, statementFilter)
            if (deparenthesized !== expression) {
                updateNumberType(numberType, deparenthesized, statementFilter, trace)
            }
            return
        }

        evaluateExpression(expression, trace, numberType)
    }

    internal fun resolveAnnotationArguments(
            resolvedCall: ResolvedCall<*>,
            trace: BindingTrace
    ): Map<ValueParameterDescriptor, ConstantValue<*>> {
        val arguments = HashMap<ValueParameterDescriptor, ConstantValue<*>>()
        for ((parameterDescriptor, resolvedArgument) in resolvedCall.getValueArguments().entrySet()) {
            val value = getAnnotationArgumentValue(trace, parameterDescriptor, resolvedArgument)
            if (value != null) {
                arguments.put(parameterDescriptor, value)
            }
        }
        return arguments
    }

    public fun getAnnotationArgumentValue(
            trace: BindingTrace,
            parameterDescriptor: ValueParameterDescriptor,
            resolvedArgument: ResolvedValueArgument
    ): ConstantValue<*>? {
        val varargElementType = parameterDescriptor.getVarargElementType()
        val argumentsAsVararg = varargElementType != null && !hasSpread(resolvedArgument)
        val constantType = if (argumentsAsVararg) varargElementType else parameterDescriptor.getType()
        val compileTimeConstants = resolveAnnotationValueArguments(resolvedArgument, constantType!!, trace)
        val constants = compileTimeConstants.map { it.toConstantValue(constantType) }

        if (argumentsAsVararg) {
            if (parameterDescriptor.declaresDefaultValue() && compileTimeConstants.isEmpty()) return null

            return constantValueFactory.createArrayValue(constants, parameterDescriptor.getType())
        }
        else {
            // we should actually get only one element, but just in case of getting many, we take the last one
            return constants.lastOrNull()
        }
    }

    private fun checkCompileTimeConstant(
            argumentExpression: JetExpression,
            expectedType: JetType,
            trace: BindingTrace
    ) {
        val expressionType = trace.getType(argumentExpression)

        if (expressionType == null || !JetTypeChecker.DEFAULT.isSubtypeOf(expressionType, expectedType)) {
            // TYPE_MISMATCH should be reported otherwise
            return
        }

        // array(1, <!>null<!>, 3) - error should be reported on inner expression
        if (argumentExpression is JetCallExpression) {
            val arrayArgument = getArgumentExpressionsForArrayCall(argumentExpression, trace)
            if (arrayArgument != null) {
                for (expression in arrayArgument.first) {
                    checkCompileTimeConstant(expression, arrayArgument.second!!, trace)
                }
            }
        }

        val constant = ConstantExpressionEvaluator.getConstant(argumentExpression, trace.getBindingContext())
        if (constant != null && constant.canBeUsedInAnnotations) {
            if (constant.usesNonConstValAsConstant) {
                trace.report(Errors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.on(argumentExpression))
            }
            return
        }

        val descriptor = expressionType.getConstructor().getDeclarationDescriptor()
        if (descriptor != null && DescriptorUtils.isEnumClass(descriptor)) {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST.on(argumentExpression))
        }
        else if (descriptor is ClassDescriptor && KotlinBuiltIns.isKClass(descriptor)) {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_KCLASS_LITERAL.on(argumentExpression))
        }
        else {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_CONST.on(argumentExpression))
        }
    }

    private fun getArgumentExpressionsForArrayCall(
            expression: JetCallExpression,
            trace: BindingTrace
    ): Pair<List<JetExpression>, JetType?>? {
        val resolvedCall = expression.getResolvedCall(trace.getBindingContext())
        if (resolvedCall == null || !CompileTimeConstantUtils.isArrayMethodCall(resolvedCall)) {
            return null
        }

        val argumentEntry = resolvedCall.getValueArguments().entrySet().single()

        val elementType = argumentEntry.getKey().getVarargElementType() ?: return null

        val result = arrayListOf<JetExpression>()
        for (valueArgument in argumentEntry.getValue().getArguments()) {
            val valueArgumentExpression = valueArgument.getArgumentExpression()
            if (valueArgumentExpression != null) {
                result.add(valueArgumentExpression)
            }
        }

        return Pair<List<JetExpression>, JetType>(result, elementType)
    }

    private fun hasSpread(argument: ResolvedValueArgument): Boolean {
        val arguments = argument.getArguments()
        return arguments.size() == 1 && arguments.get(0).getSpreadElement() != null
    }

    private fun resolveAnnotationValueArguments(
            resolvedValueArgument: ResolvedValueArgument,
            expectedType: JetType,
            trace: BindingTrace): List<CompileTimeConstant<*>> {
        val constants = ArrayList<CompileTimeConstant<*>>()
        for (argument in resolvedValueArgument.getArguments()) {
            val argumentExpression = argument.getArgumentExpression() ?: continue
            val constant = evaluateExpression(argumentExpression, trace, expectedType)
            if (constant is IntegerValueTypeConstant) {
                val defaultType = constant.getType(expectedType)
                updateNumberType(defaultType, argumentExpression, StatementFilter.NONE, trace)
            }
            if (constant != null) {
                constants.add(constant)
            }
            checkCompileTimeConstant(argumentExpression, expectedType, trace)
        }
        return constants
    }

    public fun evaluateExpression(
            expression: JetExpression,
            trace: BindingTrace,
            expectedType: JetType? = TypeUtils.NO_EXPECTED_TYPE
    ): CompileTimeConstant<*>? {
        val visitor = ConstantExpressionEvaluatorVisitor(this, trace)
        val constant = visitor.evaluate(expression, expectedType) ?: return null
        return if (!constant.isError) constant else null
    }

    public fun evaluateToConstantValue(
            expression: JetExpression,
            trace: BindingTrace,
            expectedType: JetType
    ): ConstantValue<*>? {
        return evaluateExpression(expression, trace, expectedType)?.toConstantValue(expectedType)
    }


    companion object {
        @JvmStatic
        public fun getConstant(expression: JetExpression, bindingContext: BindingContext): CompileTimeConstant<*>? {
            val constant = getPossiblyErrorConstant(expression, bindingContext) ?: return null
            return if (!constant.isError) constant else null
        }

        @JvmStatic
        fun getPossiblyErrorConstant(expression: JetExpression, bindingContext: BindingContext): CompileTimeConstant<*>? {
            return bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression)
        }
    }
}

private class ConstantExpressionEvaluatorVisitor(
        private val constantExpressionEvaluator: ConstantExpressionEvaluator,
        private val trace: BindingTrace
) : JetVisitor<CompileTimeConstant<*>?, JetType>() {

    private val factory = constantExpressionEvaluator.constantValueFactory

    fun evaluate(expression: JetExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val recordedCompileTimeConstant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, trace.getBindingContext())
        if (recordedCompileTimeConstant != null) {
            return recordedCompileTimeConstant
        }

        val compileTimeConstant = expression.accept(this, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        if (compileTimeConstant != null) {
            trace.record(BindingContext.COMPILE_TIME_VALUE, expression, compileTimeConstant)
            return compileTimeConstant
        }
        return null
    }

    private val stringExpressionEvaluator = object : JetVisitor<TypedCompileTimeConstant<String>, Nothing?>() {
        private fun createStringConstant(compileTimeConstant: CompileTimeConstant<*>): TypedCompileTimeConstant<String>? {
            val constantValue = compileTimeConstant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)
            return when (constantValue) {
                is ErrorValue, is EnumValue -> return null
                is NullValue -> factory.createStringValue("null")
                else -> factory.createStringValue(constantValue.value.toString())
            }.wrap(compileTimeConstant.parameters)
        }

        fun evaluate(entry: JetStringTemplateEntry): TypedCompileTimeConstant<String>? {
            return entry.accept(this, null)
        }

        override fun visitStringTemplateEntryWithExpression(entry: JetStringTemplateEntryWithExpression, data: Nothing?): TypedCompileTimeConstant<String>? {
            val expression = entry.getExpression() ?: return null

            return evaluate(expression, constantExpressionEvaluator.builtIns.getStringType())?.let {
                createStringConstant(it)
            }
        }

        override fun visitLiteralStringTemplateEntry(entry: JetLiteralStringTemplateEntry, data: Nothing?) = factory.createStringValue(entry.getText()).wrap()

        override fun visitEscapeStringTemplateEntry(entry: JetEscapeStringTemplateEntry, data: Nothing?) = factory.createStringValue(entry.getUnescapedValue()).wrap()
    }

    override fun visitConstantExpression(expression: JetConstantExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val text = expression.getText() ?: return null

        val nodeElementType = expression.getNode().getElementType()
        if (nodeElementType == JetNodeTypes.NULL) return factory.createNullValue().wrap()

        val result: Any? = when (nodeElementType) {
                               JetNodeTypes.INTEGER_CONSTANT -> parseLong(text)
                               JetNodeTypes.FLOAT_CONSTANT -> parseFloatingLiteral(text)
                               JetNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
                               JetNodeTypes.CHARACTER_CONSTANT -> CompileTimeConstantChecker.parseChar(expression)
                               else -> throw IllegalArgumentException("Unsupported constant: " + expression)
                           } ?: return null

        fun isLongWithSuffix() = nodeElementType == JetNodeTypes.INTEGER_CONSTANT && hasLongSuffix(text)
        return createConstant(result, expectedType, CompileTimeConstant.Parameters(true, !isLongWithSuffix(), false, usesNonConstValAsConstant = false))
    }

    override fun visitParenthesizedExpression(expression: JetParenthesizedExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val deparenthesizedExpression = JetPsiUtil.deparenthesize(expression)
        if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
            return evaluate(deparenthesizedExpression, expectedType)
        }
        return null
    }

    override fun visitLabeledExpression(expression: JetLabeledExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val baseExpression = expression.getBaseExpression()
        if (baseExpression != null) {
            return evaluate(baseExpression, expectedType)
        }
        return null
    }

    override fun visitStringTemplateExpression(expression: JetStringTemplateExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val sb = StringBuilder()
        var interupted = false
        var canBeUsedInAnnotation = true
        var usesVariableAsConstant = false
        var usesNonConstantVariableAsConstant = false
        for (entry in expression.getEntries()) {
            val constant = stringExpressionEvaluator.evaluate(entry)
            if (constant == null) {
                interupted = true
                break
            }
            else {
                if (!constant.canBeUsedInAnnotations) canBeUsedInAnnotation = false
                if (constant.usesVariableAsConstant) usesVariableAsConstant = true
                if (constant.usesNonConstValAsConstant) usesNonConstantVariableAsConstant = true
                sb.append(constant.constantValue.value)
            }
        }
        return if (!interupted)
            createConstant(
                    sb.toString(),
                    expectedType,
                    CompileTimeConstant.Parameters(
                            isPure = false,
                            canBeUsedInAnnotation = canBeUsedInAnnotation,
                            usesVariableAsConstant = usesVariableAsConstant,
                            usesNonConstValAsConstant = usesNonConstantVariableAsConstant
                    )
            )
        else null
    }

    override fun visitBinaryWithTypeRHSExpression(expression: JetBinaryExpressionWithTypeRHS, expectedType: JetType?): CompileTimeConstant<*>? =
            evaluate(expression.getLeft(), expectedType)

    override fun visitBinaryExpression(expression: JetBinaryExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val leftExpression = expression.getLeft() ?: return null

        val operationToken = expression.getOperationToken()
        if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
            val booleanType = constantExpressionEvaluator.builtIns.getBooleanType()
            val leftConstant = evaluate(leftExpression, booleanType)
            if (leftConstant == null) return null

            val rightExpression = expression.getRight() ?: return null

            val rightConstant = evaluate(rightExpression, booleanType) ?: return null

            val leftValue = leftConstant.getValue(booleanType)
            val rightValue = rightConstant.getValue(booleanType)

            if (leftValue !is Boolean || rightValue !is Boolean) return null
            val result = when (operationToken) {
                JetTokens.ANDAND -> leftValue && rightValue
                JetTokens.OROR -> leftValue || rightValue
                else -> throw IllegalArgumentException("Unknown boolean operation token ${operationToken}")
            }
            return createConstant(
                    result, expectedType,
                    CompileTimeConstant.Parameters(
                            canBeUsedInAnnotation = true,
                            isPure = false,
                            usesVariableAsConstant = leftConstant.usesVariableAsConstant || rightConstant.usesVariableAsConstant,
                            usesNonConstValAsConstant = leftConstant.usesNonConstValAsConstant || rightConstant.usesNonConstValAsConstant
                    )
            )
        }
        else {
            return evaluateCall(expression.getOperationReference(), leftExpression, expectedType)
        }
    }

    private fun evaluateCall(callExpression: JetExpression, receiverExpression: JetExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val resolvedCall = callExpression.getResolvedCall(trace.getBindingContext())
        if (resolvedCall == null) return null

        val resultingDescriptorName = resolvedCall.getResultingDescriptor().getName()

        val argumentForReceiver = createOperationArgumentForReceiver(resolvedCall, receiverExpression)
        if (argumentForReceiver == null) return null

        val argumentsEntrySet = resolvedCall.getValueArguments().entrySet()
        if (argumentsEntrySet.isEmpty()) {
            val result = evaluateUnaryAndCheck(argumentForReceiver, resultingDescriptorName.asString(), callExpression)
            if (result == null) return null
            val isArgumentPure = isPureConstant(argumentForReceiver.expression)
            val canBeUsedInAnnotation = canBeUsedInAnnotation(argumentForReceiver.expression)
            val usesVariableAsConstant = usesVariableAsConstant(argumentForReceiver.expression)
            val usesNonConstValAsConstant = usesNonConstValAsConstant(argumentForReceiver.expression)
            val isNumberConversionMethod = resultingDescriptorName in OperatorConventions.NUMBER_CONVERSIONS
            return createConstant(
                    result,
                    expectedType,
                    CompileTimeConstant.Parameters(
                            canBeUsedInAnnotation,
                            !isNumberConversionMethod && isArgumentPure,
                            usesVariableAsConstant, usesNonConstValAsConstant)
            )
        }
        else if (argumentsEntrySet.size() == 1) {
            val (parameter, argument) = argumentsEntrySet.first()
            val argumentForParameter = createOperationArgumentForFirstParameter(argument, parameter)
            if (argumentForParameter == null) return null

            if (isDivisionByZero(resultingDescriptorName.asString(), argumentForParameter.value)) {
                val parentExpression: JetExpression = PsiTreeUtil.getParentOfType(receiverExpression, javaClass())!!
                trace.report(Errors.DIVISION_BY_ZERO.on(parentExpression))
                return factory.createErrorValue("Division by zero").wrap()
            }

            val result = evaluateBinaryAndCheck(argumentForReceiver, argumentForParameter, resultingDescriptorName.asString(), callExpression)
            if (result == null) return null

            val areArgumentsPure = isPureConstant(argumentForReceiver.expression) && isPureConstant(argumentForParameter.expression)
            val canBeUsedInAnnotation = canBeUsedInAnnotation(argumentForReceiver.expression) && canBeUsedInAnnotation(argumentForParameter.expression)
            val usesVariableAsConstant = usesVariableAsConstant(argumentForReceiver.expression) || usesVariableAsConstant(argumentForParameter.expression)
            val usesNonConstValAsConstant = usesNonConstValAsConstant(argumentForReceiver.expression) || usesNonConstValAsConstant(argumentForParameter.expression)
            val parameters = CompileTimeConstant.Parameters(canBeUsedInAnnotation, areArgumentsPure, usesVariableAsConstant, usesNonConstValAsConstant)
            return when (resultingDescriptorName) {
                OperatorConventions.COMPARE_TO -> createCompileTimeConstantForCompareTo(result, callExpression, factory)?.wrap(parameters)
                OperatorConventions.EQUALS -> createCompileTimeConstantForEquals(result, callExpression, factory)?.wrap(parameters)
                else -> {
                    createConstant(result, expectedType, parameters)
                }
            }
        }

        return null
    }

    private fun usesVariableAsConstant(expression: JetExpression) = ConstantExpressionEvaluator.getConstant(expression, trace.getBindingContext())?.usesVariableAsConstant ?: false
    private fun usesNonConstValAsConstant(expression: JetExpression)
            = ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesNonConstValAsConstant ?: false

    private fun canBeUsedInAnnotation(expression: JetExpression) = ConstantExpressionEvaluator.getConstant(expression, trace.getBindingContext())?.canBeUsedInAnnotations ?: false

    private fun isPureConstant(expression: JetExpression) = ConstantExpressionEvaluator.getConstant(expression, trace.getBindingContext())?.isPure ?: false

    private fun evaluateUnaryAndCheck(receiver: OperationArgument, name: String, callExpression: JetExpression): Any? {
        val functions = unaryOperations[UnaryOperationKey(receiver.ctcType, name)]
        if (functions == null) return null

        val (function, check) = functions
        val result = function(receiver.value)
        if (check == emptyUnaryFun) {
            return result
        }
        assert (isIntegerType(receiver.value), "Only integer constants should be checked for overflow")
        assert (name == "minus", "Only negation should be checked for overflow")

        if (receiver.value == result) {
            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType<JetExpression>() ?: callExpression))
        }
        return result
    }

    private fun evaluateBinaryAndCheck(receiver: OperationArgument, parameter: OperationArgument, name: String, callExpression: JetExpression): Any? {
        val functions = binaryOperations[BinaryOperationKey(receiver.ctcType, parameter.ctcType, name)]
        if (functions == null) return null

        val (function, checker) = functions
        val actualResult = try {
            function(receiver.value, parameter.value)
        }
        catch (e: Exception) {
            null
        }
        if (checker == emptyBinaryFun) {
            return actualResult
        }
        assert (isIntegerType(receiver.value) && isIntegerType(parameter.value)) { "Only integer constants should be checked for overflow" }

        fun toBigInteger(value: Any?) = BigInteger.valueOf((value as Number).toLong())

        val resultInBigIntegers = checker(toBigInteger(receiver.value), toBigInteger(parameter.value))

        if (toBigInteger(actualResult) != resultInBigIntegers) {
            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType<JetExpression>() ?: callExpression))
        }
        return actualResult
    }

    private fun isDivisionByZero(name: String, parameter: Any?): Boolean {
        if (name == OperatorConventions.BINARY_OPERATION_NAMES[JetTokens.DIV]!!.asString()) {
            if (isIntegerType(parameter)) {
                return (parameter as Number).toLong() == 0.toLong()
            }
            else if (parameter is Float || parameter is Double) {
                return (parameter as Number).toDouble() == 0.0
            }
        }
        return false
    }

    override fun visitUnaryExpression(expression: JetUnaryExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val leftExpression = expression.getBaseExpression()
        if (leftExpression == null) return null

        return evaluateCall(expression.getOperationReference(), leftExpression, expectedType)
    }

    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val enumDescriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, expression);
        if (enumDescriptor != null && DescriptorUtils.isEnumEntry(enumDescriptor)) {
            return factory.createEnumValue(enumDescriptor as ClassDescriptor).wrap()
        }

        val resolvedCall = expression.getResolvedCall(trace.getBindingContext())
        if (resolvedCall != null) {
            val callableDescriptor = resolvedCall.getResultingDescriptor()
            if (callableDescriptor is VariableDescriptor) {
                val variableInitializer = callableDescriptor.getCompileTimeInitializer() ?: return null

                return createConstant(
                        variableInitializer.value,
                        expectedType,
                        CompileTimeConstant.Parameters(
                                canBeUsedInAnnotation = isPropertyCompileTimeConstant(callableDescriptor),
                                isPure = false,
                                usesVariableAsConstant = true,
                                usesNonConstValAsConstant = !callableDescriptor.isConst
                        )
                )
            }
        }
        return null
    }

    // TODO: Should be replaced with descriptor.isConst
    private fun isPropertyCompileTimeConstant(descriptor: VariableDescriptor): Boolean {
        if (descriptor.isVar()) {
            return false
        }
        if (DescriptorUtils.isObject(descriptor.getContainingDeclaration()) ||
            DescriptorUtils.isStaticDeclaration(descriptor)) {
            return descriptor.type.canBeUsedForConstVal()
        }
        return false
    }

    override fun visitQualifiedExpression(expression: JetQualifiedExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val selectorExpression = expression.getSelectorExpression()
        // 1.toInt(); 1.plus(1);
        if (selectorExpression is JetCallExpression) {
            val qualifiedCallValue = evaluate(selectorExpression, expectedType)
            if (qualifiedCallValue != null) {
                return qualifiedCallValue
            }

            val calleeExpression = selectorExpression.getCalleeExpression()
            if (calleeExpression !is JetSimpleNameExpression) {
                return null
            }

            val receiverExpression = expression.getReceiverExpression()
            return evaluateCall(calleeExpression, receiverExpression, expectedType)
        }

        // MyEnum.A, Integer.MAX_VALUE
        if (selectorExpression != null) {
            return evaluate(selectorExpression, expectedType)
        }

        return null
    }

    override fun visitCallExpression(expression: JetCallExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val call = expression.getResolvedCall(trace.getBindingContext())
        if (call == null) return null

        val resultingDescriptor = call.getResultingDescriptor()

        // arrayOf()
        if (CompileTimeConstantUtils.isArrayMethodCall(call)) {
            val varargType = resultingDescriptor.getValueParameters().first().getVarargElementType()!!

            val arguments = call.getValueArguments().values().flatMap { resolveArguments(it.getArguments(), varargType) }

            return factory.createArrayValue(arguments.map { it.toConstantValue(varargType) }, resultingDescriptor.getReturnType()!!).
                    wrap(
                            usesVariableAsConstant = arguments.any { it.usesVariableAsConstant },
                            usesNonConstValAsConstant = arguments.any { it.usesNonConstValAsConstant }
                    )
        }

        // Ann()
        if (resultingDescriptor is ConstructorDescriptor) {
            val classDescriptor: ClassDescriptor = resultingDescriptor.getContainingDeclaration()
            if (DescriptorUtils.isAnnotationClass(classDescriptor)) {
                val descriptor = AnnotationDescriptorImpl(
                        classDescriptor.getDefaultType(),
                        constantExpressionEvaluator.resolveAnnotationArguments(call, trace),
                        SourceElement.NO_SOURCE
                )
                return AnnotationValue(descriptor).wrap()
            }
        }

        return null
    }

    override fun visitClassLiteralExpression(expression: JetClassLiteralExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val jetType = trace.getType(expression)!!
        if (jetType.isError()) return null
        return KClassValue(jetType).wrap()
    }

    private fun resolveArguments(valueArguments: List<ValueArgument>, expectedType: JetType): List<CompileTimeConstant<*>> {
        val constants = arrayListOf<CompileTimeConstant<*>>()
        for (argument in valueArguments) {
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression != null) {
                val compileTimeConstant = evaluate(argumentExpression, expectedType)
                if (compileTimeConstant != null) {
                    constants.add(compileTimeConstant)
                }
            }
        }
        return constants
    }

    override fun visitJetElement(element: JetElement, expectedType: JetType?): CompileTimeConstant<*>? {
        return null
    }

    private class OperationArgument(val value: Any, val ctcType: CompileTimeType<*>, val expression: JetExpression)

    private fun createOperationArgumentForReceiver(resolvedCall: ResolvedCall<*>, expression: JetExpression): OperationArgument? {
        val receiverExpressionType = getReceiverExpressionType(resolvedCall)
        if (receiverExpressionType == null) return null

        val receiverCompileTimeType = getCompileTimeType(receiverExpressionType)
        if (receiverCompileTimeType == null) return null

        return createOperationArgument(expression, receiverExpressionType, receiverCompileTimeType)
    }

    private fun createOperationArgumentForFirstParameter(argument: ResolvedValueArgument, parameter: ValueParameterDescriptor): OperationArgument? {
        val argumentCompileTimeType = getCompileTimeType(parameter.getType())
        if (argumentCompileTimeType == null) return null

        val arguments = argument.getArguments()
        if (arguments.size() != 1) return null

        val argumentExpression = arguments.first().getArgumentExpression()
        if (argumentExpression == null) return null
        return createOperationArgument(argumentExpression, parameter.getType(), argumentCompileTimeType)
    }

    private fun createOperationArgument(expression: JetExpression, expressionType: JetType, compileTimeType: CompileTimeType<*>): OperationArgument? {
        val compileTimeConstant = constantExpressionEvaluator.evaluateExpression(expression, trace, expressionType) ?: return null
        val evaluationResult = compileTimeConstant.getValue(expressionType) ?: return null
        return OperationArgument(evaluationResult, compileTimeType, expression)
    }

    private fun createConstant(
            value: Any?,
            expectedType: JetType?,
            parameters: CompileTimeConstant.Parameters
    ): CompileTimeConstant<*>? {
        return if (parameters.isPure) {
            return createCompileTimeConstant(value, parameters, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        }
        else {
            factory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun createCompileTimeConstant(
            value: Any?,
            parameters: CompileTimeConstant.Parameters,
            expectedType: JetType
    ): CompileTimeConstant<*>? {
        return when (value) {
            is Byte, is Short, is Int, is Long -> createIntegerCompileTimeConstant((value as Number).toLong(), parameters, expectedType)
            else -> factory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun createIntegerCompileTimeConstant(
            value: Long,
            parameters: CompileTimeConstant.Parameters,
            expectedType: JetType
    ): CompileTimeConstant<*>? {
        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
            return IntegerValueTypeConstant(value, constantExpressionEvaluator.builtIns, parameters)
        }
        val integerValue = factory.createIntegerConstantValue(value, expectedType)
        if (integerValue != null) {
            return integerValue.wrap(parameters)
        }
        return when (value) {
            value.toInt().toLong() -> factory.createIntValue(value.toInt())
            else -> factory.createLongValue(value)
        }.wrap(parameters)
    }

    private fun <T> ConstantValue<T>.wrap(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<T>
            = TypedCompileTimeConstant(this, parameters)

    private fun <T> ConstantValue<T>.wrap(
            canBeUsedInAnnotation: Boolean = this !is NullValue,
            isPure: Boolean = false,
            usesVariableAsConstant: Boolean = false,
            usesNonConstValAsConstant: Boolean = false
    ): TypedCompileTimeConstant<T>
            = wrap(CompileTimeConstant.Parameters(canBeUsedInAnnotation, isPure, usesVariableAsConstant, usesNonConstValAsConstant))
}

private fun hasLongSuffix(text: String) = text.endsWith('l') || text.endsWith('L')

public fun parseLong(text: String): Long? {
    try {
        fun substringLongSuffix(s: String) = if (hasLongSuffix(text)) s.substring(0, s.length() - 1) else s
        fun parseLong(text: String, radix: Int) = java.lang.Long.parseLong(substringLongSuffix(text), radix)

        return when {
            text.startsWith("0x") || text.startsWith("0X") -> parseLong(text.substring(2), 16)
            text.startsWith("0b") || text.startsWith("0B") -> parseLong(text.substring(2), 2)
            else -> parseLong(text, 10)
        }
    }
    catch (e: NumberFormatException) {
        return null
    }
}

private fun parseFloatingLiteral(text: String): Any? {
    if (text.toLowerCase().endsWith('f')) {
        return parseFloat(text)
    }
    return parseDouble(text)
}

private fun parseDouble(text: String): Double? {
    try {
        return java.lang.Double.parseDouble(text)
    }
    catch (e: NumberFormatException) {
        return null
    }
}

private fun parseFloat(text: String): Float? {
    try {
        return java.lang.Float.parseFloat(text)
    }
    catch (e: NumberFormatException) {
        return null
    }
}

private fun parseBoolean(text: String): Boolean {
    if ("true".equals(text)) {
        return true
    }
    else if ("false".equals(text)) {
        return false
    }

    throw IllegalStateException("Must not happen. A boolean literal has text: " + text)
}


private fun createCompileTimeConstantForEquals(result: Any?, operationReference: JetExpression, factory: ConstantValueFactory): ConstantValue<*>? {
    if (result is Boolean) {
        assert(operationReference is JetSimpleNameExpression, "This method should be called only for equals operations")
        val operationToken = (operationReference as JetSimpleNameExpression).getReferencedNameElementType()
        val value: Boolean = when (operationToken) {
            JetTokens.EQEQ -> result
            JetTokens.EXCLEQ -> !result
            JetTokens.IDENTIFIER -> {
                assert (operationReference.getReferencedNameAsName() == OperatorConventions.EQUALS, "This method should be called only for equals operations")
                result
            }
            else -> throw IllegalStateException("Unknown equals operation token: $operationToken ${operationReference.getText()}")
        }
        return factory.createBooleanValue(value)
    }
    return null
}

private fun createCompileTimeConstantForCompareTo(result: Any?, operationReference: JetExpression, factory: ConstantValueFactory): ConstantValue<*>? {
    if (result is Int) {
        assert(operationReference is JetSimpleNameExpression, "This method should be called only for compareTo operations")
        val operationToken = (operationReference as JetSimpleNameExpression).getReferencedNameElementType()
        return when (operationToken) {
            JetTokens.LT -> factory.createBooleanValue(result < 0)
            JetTokens.LTEQ -> factory.createBooleanValue(result <= 0)
            JetTokens.GT -> factory.createBooleanValue(result > 0)
            JetTokens.GTEQ -> factory.createBooleanValue(result >= 0)
            JetTokens.IDENTIFIER -> {
                assert (operationReference.getReferencedNameAsName() == OperatorConventions.COMPARE_TO, "This method should be called only for compareTo operations")
                return factory.createIntValue(result)
            }
            else -> throw IllegalStateException("Unknown compareTo operation token: $operationToken")
        }
    }
    return null
}

fun isIntegerType(value: Any?) = value is Byte || value is Short || value is Int || value is Long

private fun getReceiverExpressionType(resolvedCall: ResolvedCall<*>): JetType? {
    return when (resolvedCall.getExplicitReceiverKind()) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> resolvedCall.getDispatchReceiver().getType()
        ExplicitReceiverKind.EXTENSION_RECEIVER -> resolvedCall.getExtensionReceiver().getType()
        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> null
        ExplicitReceiverKind.BOTH_RECEIVERS -> null
        else -> null
    }
}

private fun getCompileTimeType(c: JetType): CompileTimeType<out Any>? {
    val builtIns = KotlinBuiltIns.getInstance()
    return when (TypeUtils.makeNotNullable(c)) {
        builtIns.getIntType() -> INT
        builtIns.getByteType() -> BYTE
        builtIns.getShortType() -> SHORT
        builtIns.getLongType() -> LONG
        builtIns.getDoubleType() -> DOUBLE
        builtIns.getFloatType() -> FLOAT
        builtIns.getCharType() -> CHAR
        builtIns.getBooleanType() -> BOOLEAN
        builtIns.getStringType() -> STRING
        builtIns.getAnyType() -> ANY
        else -> null
    }
}

private class CompileTimeType<T>

internal val BYTE = CompileTimeType<Byte>()
internal val SHORT = CompileTimeType<Short>()
internal val INT = CompileTimeType<Int>()
internal val LONG = CompileTimeType<Long>()
internal val DOUBLE = CompileTimeType<Double>()
internal val FLOAT = CompileTimeType<Float>()
internal val CHAR = CompileTimeType<Char>()
internal val BOOLEAN = CompileTimeType<Boolean>()
internal val STRING = CompileTimeType<String>()
internal val ANY = CompileTimeType<Any>()

@Suppress("UNCHECKED_CAST")
internal fun <A, B> binaryOperation(
        a: CompileTimeType<A>,
        b: CompileTimeType<B>,
        functionName: String,
        operation: Function2<A, B, Any>,
        checker: Function2<BigInteger, BigInteger, BigInteger>
) = BinaryOperationKey(a, b, functionName) to Pair(operation, checker) as Pair<Function2<Any?, Any?, Any>, Function2<BigInteger, BigInteger, BigInteger>>

@Suppress("UNCHECKED_CAST")
internal fun <A> unaryOperation(
        a: CompileTimeType<A>,
        functionName: String,
        operation: Function1<A, Any>,
        checker: Function1<Long, Long>
) = UnaryOperationKey(a, functionName) to Pair(operation, checker) as Pair<Function1<Any?, Any>, Function1<Long, Long>>

internal data class BinaryOperationKey<A, B>(val f: CompileTimeType<out A>, val s: CompileTimeType<out B>, val functionName: String)
internal data class UnaryOperationKey<A>(val f: CompileTimeType<out A>, val functionName: String)
