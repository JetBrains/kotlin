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

package org.jetbrains.jet.lang.evaluate

import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.resolve.BindingContext.COMPILE_TIME_INITIALIZER
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.jet.JetNodeTypes
import java.lang.Long.parseLong as javaParseLong

[suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
public class ConstantExpressionEvaluator private (val trace: BindingTrace) : JetVisitor<CompileTimeConstant<*>, JetType>() {

    class object {
        public fun evaluate(expression: JetExpression, trace: BindingTrace, expectedType: JetType? = TypeUtils.NO_EXPECTED_TYPE): CompileTimeConstant<*>? {
            val evaluator = ConstantExpressionEvaluator(trace)
            return evaluator.evaluate(expression, expectedType)
        }
    }

    private fun evaluate(expression: JetExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val recordedCompileTimeConstant = trace.get(BindingContext.COMPILE_TIME_VALUE, expression)
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

    private val stringExpressionEvaluator = object : JetVisitor<StringValue, Nothing>() {
        fun evaluate(entry: JetStringTemplateEntry): StringValue? {
            return entry.accept(this, null)
        }

        override fun visitStringTemplateEntryWithExpression(entry: JetStringTemplateEntryWithExpression, data: Nothing?): StringValue? {
            val expression = entry.getExpression()
            if (expression == null) return null

            return createStringConstant(this@ConstantExpressionEvaluator.evaluate(expression, KotlinBuiltIns.getInstance().getStringType()))
        }

        override fun visitLiteralStringTemplateEntry(entry: JetLiteralStringTemplateEntry, data: Nothing?) = StringValue(entry.getText())

        override fun visitEscapeStringTemplateEntry(entry: JetEscapeStringTemplateEntry, data: Nothing?) = StringValue(entry.getUnescapedValue())
    }

    override fun visitConstantExpression(expression: JetConstantExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val text = expression.getText()
        if (text == null) return null
        val result: Any? = when (expression.getNode().getElementType()) {
            JetNodeTypes.INTEGER_CONSTANT -> parseLong(text)
            JetNodeTypes.FLOAT_CONSTANT -> parseDouble(text)
            JetNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
            JetNodeTypes.CHARACTER_CONSTANT -> CompileTimeConstantResolver.parseChar(expression)
            JetNodeTypes.NULL -> null
            else -> throw IllegalArgumentException("Unsupported constant: " + expression)
        }
        if (result == null && expression.getNode().getElementType() == JetNodeTypes.NULL) return NullValue.NULL

        return createCompileTimeConstant(result, expectedType)
    }

    override fun visitParenthesizedExpression(expression: JetParenthesizedExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val deparenthesizedExpression = JetPsiUtil.deparenthesize(expression)
        if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
            return evaluate(deparenthesizedExpression, expectedType)
        }
        return null
    }

    override fun visitPrefixExpression(expression: JetPrefixExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val deparenthesizedExpression = JetPsiUtil.deparenthesize(expression)
        return if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
            evaluate(deparenthesizedExpression, expectedType)
        }
        else {
            super.visitPrefixExpression(expression, expectedType)
        }
    }

    override fun visitStringTemplateExpression(expression: JetStringTemplateExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val sb = StringBuilder()
        var interupted = false
        for (entry in expression.getEntries()) {
            val constant = stringExpressionEvaluator.evaluate(entry)
            if (constant == null) {
                interupted = true
                break
            }
            else {
                sb.append(constant.getValue())
            }
        }
        return if (!interupted) createCompileTimeConstant(sb.toString(), expectedType) else null
    }

    override fun visitBinaryExpression(expression: JetBinaryExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val leftExpression = expression.getLeft()
        if (leftExpression == null) return null

        val operationToken = expression.getOperationToken()
        if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
            val booleanType = KotlinBuiltIns.getInstance().getBooleanType()
            val leftConstant = evaluate(leftExpression, booleanType)
            if (leftConstant == null) return null

            val rightExpression = expression.getRight()
            if (rightExpression == null) return null

            val rightConstant = evaluate(rightExpression, booleanType)
            if (rightConstant == null) return null

            val leftValue = leftConstant.getValue()
            val rightValue = rightConstant.getValue()

            if (leftValue !is Boolean || rightValue !is Boolean) return null
            val result = when(operationToken) {
                JetTokens.ANDAND -> leftValue as Boolean && rightValue as Boolean
                JetTokens.OROR -> leftValue as Boolean || rightValue as Boolean
                else -> throw IllegalArgumentException("Unknown boolean operation token ${operationToken}")
            }
            return createCompileTimeConstant(result, expectedType)
        }
        else {
            val result = evaluateCall(expression.getOperationReference(), leftExpression)
            return when(operationToken) {
                in OperatorConventions.COMPARISON_OPERATIONS -> createCompileTimeConstantForCompareTo(result, operationToken!!)
                in OperatorConventions.EQUALS_OPERATIONS -> createCompileTimeConstantForEquals(result, operationToken!!)
                else -> createCompileTimeConstant(result, expectedType)
            }
        }
    }

    private fun evaluateCall(callExpression: JetExpression, receiverExpression: JetExpression): Any? {
        val resolvedCall = trace.getBindingContext().get(BindingContext.RESOLVED_CALL, callExpression)
        if (resolvedCall == null) return null

        val resultingDescriptorName = resolvedCall.getResultingDescriptor()?.getName()?.asString()
        if (resultingDescriptorName == null) return null

        val argumentForReceiver = createOperationArgumentForReceiver(resolvedCall, receiverExpression)
        if (argumentForReceiver == null) return null

        val argumentsEntrySet = resolvedCall.getValueArguments().entrySet()
        if (argumentsEntrySet.isEmpty()) {
            val function = unaryOperations[UnaryOperationKey(argumentForReceiver.ctcType, resultingDescriptorName)]
            if (function == null) return null
            return function(argumentForReceiver.value)
        }
        else if (argumentsEntrySet.size() == 1) {
            val (parameter, argument) = argumentsEntrySet.first()

            val argumentForParameter = createOperationArgumentForFirstParameter(argument, parameter)
            if (argumentForParameter == null) return null

            val function = binaryOperations[BinaryOperationKey(argumentForReceiver.ctcType, argumentForParameter.ctcType, resultingDescriptorName)]
            if (function == null) return null
            return function(argumentForReceiver.value, argumentForParameter.value)
        }

        return null
    }

    override fun visitUnaryExpression(expression: JetUnaryExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val leftExpression = expression.getBaseExpression()
        if (leftExpression == null) return null
        val result = evaluateCall(expression.getOperationReference(), leftExpression)
        return createCompileTimeConstant(result, expectedType)
    }

    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val enumDescriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, expression);
        if (enumDescriptor != null && DescriptorUtils.isEnumEntry(enumDescriptor)) {
            return EnumValue(enumDescriptor as ClassDescriptor);
        }

        val resolvedCall = trace.getBindingContext().get(BindingContext.RESOLVED_CALL, expression)
        if (resolvedCall != null) {
            val callableDescriptor = resolvedCall.getResultingDescriptor()
            if (callableDescriptor is PropertyDescriptor) {
                if (AnnotationUtils.isPropertyCompileTimeConstant(callableDescriptor)) {
                    return trace.getBindingContext().get(COMPILE_TIME_INITIALIZER, callableDescriptor)
                }
            }
        }
        return null
    }

    override fun visitQualifiedExpression(expression: JetQualifiedExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val selectorExpression = expression.getSelectorExpression()
        // 1.toInt(); 1.plus(1);
        if (selectorExpression is JetCallExpression) {
            val calleeExpression = selectorExpression.getCalleeExpression()
            if (calleeExpression !is JetSimpleNameExpression) {
                return null
            }

            val receiverExpression = expression.getReceiverExpression()
            val result = evaluateCall(calleeExpression, receiverExpression)
            return createCompileTimeConstant(result, expectedType)
        }

        // Mynum.A
        if (selectorExpression != null) {
            return evaluate(selectorExpression, expectedType)
        }

        return null
    }

    override fun visitCallExpression(expression: JetCallExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        val call = trace.getBindingContext().get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression())
        if (call == null) return null

        val resultingDescriptor = call.getResultingDescriptor()
        if (resultingDescriptor == null) return null

        // array()
        if (AnnotationUtils.isArrayMethodCall(call)) {
            val varargType = resultingDescriptor.getValueParameters().first?.getVarargElementType()!!

            val arguments = call.getValueArguments().values().flatMap { resolveArguments(it.getArguments(), varargType) }
            return ArrayValue(arguments, resultingDescriptor.getReturnType()!!)
        }

        // Ann()
        if (resultingDescriptor is ConstructorDescriptor) {
            val classDescriptor: ClassDescriptor = resultingDescriptor.getContainingDeclaration()
            if (DescriptorUtils.isAnnotationClass(classDescriptor)) {
                val descriptor = AnnotationDescriptorImpl()
                descriptor.setAnnotationType(classDescriptor.getDefaultType())
                AnnotationResolver.resolveAnnotationArgument(descriptor, call, trace)
                return AnnotationValue(descriptor)
            }
        }

        // javaClass()
        if (AnnotationUtils.isJavaClassMethodCall(call)) {
            return JavaClassValue(resultingDescriptor.getReturnType())
        }

        return null
    }

    private fun resolveArguments(valueArguments: List<ValueArgument>, expectedType: JetType): List<CompileTimeConstant<*>> {
        val constants = arrayListOf<CompileTimeConstant<*>>()
        for (argument in valueArguments) {
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression != null) {
                val constant = evaluate(argumentExpression, expectedType)
                if (constant != null) {
                    constants.add(constant)
                }
            }
        }
        return constants
    }

    override fun visitJetElement(element: JetElement, expectedType: JetType?): CompileTimeConstant<*>? {
        return null
    }


    private class OperationArgument(val value: Any?, val ctcType: CompileTimeType<*>)

    private fun createOperationArgumentForReceiver(resolvedCall: ResolvedCall<*>, expression: JetExpression): OperationArgument? {
        val receiverExpressionType = getReceiverExpressionType(resolvedCall)
        if (receiverExpressionType == null) return null

        val receiverCompileTimeType = getCompileTimeType(receiverExpressionType)
        if (receiverCompileTimeType == null) return null

        val receiverValue = evaluate(expression, receiverExpressionType)?.getValue()
        if (receiverValue == null) return null

        return OperationArgument(receiverValue, receiverCompileTimeType)
    }

    private fun createOperationArgumentForFirstParameter(argument: ResolvedValueArgument, parameter: ValueParameterDescriptor): OperationArgument? {
        val argumentCompileTimeType = getCompileTimeType(parameter.getType())
        if (argumentCompileTimeType == null) return null

        val argumentCompileTimeValue = resolveArguments(argument.getArguments(), parameter.getType())
        if (argumentCompileTimeValue.size != 1) return null

        val argumentValue = argumentCompileTimeValue.first().getValue()
        if (argumentValue == null) return null

        return OperationArgument(argumentValue, argumentCompileTimeType)
    }
}

public fun parseLong(text: String): Long? {
    try {
        return when {
            text.startsWith("0x") || text.startsWith("0X") -> javaParseLong(text.substring(2), 16)
            text.startsWith("0b") || text.startsWith("0B") -> javaParseLong(text.substring(2), 2)
            else -> javaParseLong(text)
        }
    }
    catch (e: NumberFormatException) {
        return null
    }
}

private fun parseDouble(text: String): Double? {
    try {
        return java.lang.Double.parseDouble(text)
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


private fun createCompileTimeConstantForEquals(result: Any?, operationToken: IElementType): CompileTimeConstant<*>? {
    if (result is Boolean) {
        return when (operationToken) {
            JetTokens.EQEQ -> BooleanValue.valueOf(result)
            JetTokens.EXCLEQ -> BooleanValue.valueOf(!result)
            else -> throw IllegalStateException("Unknown equals operation token: $operationToken")
        }
    }
    return null
}

private fun createCompileTimeConstantForCompareTo(result: Any?, operationToken: IElementType): CompileTimeConstant<*>? {
    if (result is Int) {
        return when (operationToken) {
            JetTokens.LT -> BooleanValue.valueOf(result < 0)
            JetTokens.LTEQ -> BooleanValue.valueOf(result <= 0)
            JetTokens.GT -> BooleanValue.valueOf(result > 0)
            JetTokens.GTEQ -> BooleanValue.valueOf(result >= 0)
            else -> throw IllegalStateException("Unknown compareTo operation token: $operationToken")
        }
    }
    return null
}

private fun createStringConstant(value: CompileTimeConstant<*>?): StringValue? {
    return when (value) {
        null -> null
        is StringValue -> value
        is IntValue, is ByteValue, is ShortValue, is LongValue,
        is CharValue,
        is DoubleValue, is FloatValue,
        is BooleanValue -> StringValue(value.getValue().toString())
        else -> null
    }
}

public fun createCompileTimeConstant(value: Any?, expectedType: JetType?): CompileTimeConstant<*>? {
    return when(value) {
        null -> null
        is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        is Char -> CharValue(value)
        is Float -> FloatValue(value)
        is Double -> DoubleValue(value)
        is Boolean -> BooleanValue.valueOf(value)
        is String -> StringValue(value)
        else -> null
    }
}

private fun getIntegerValue(value: Long, expectedType: JetType): CompileTimeConstant<*>? {
    fun defaultIntegerValue(value: Long) = when (value) {
        value.toInt().toLong() -> IntValue(value.toInt())
        else -> LongValue(value)
    }

    if (CompileTimeConstantResolver.noExpectedTypeOrError(expectedType)) {
        return defaultIntegerValue(value)
    }

    val builtIns = KotlinBuiltIns.getInstance()

    return when (TypeUtils.makeNotNullable(expectedType)) {
        builtIns.getLongType() -> LongValue(value)
        builtIns.getShortType() -> when (value) {
            value.toShort().toLong() -> ShortValue(value.toShort())
            else -> defaultIntegerValue(value)
        }
        builtIns.getByteType() -> when (value) {
            value.toByte().toLong() -> ByteValue(value.toByte())
            else -> defaultIntegerValue(value)
        }
        builtIns.getCharType() -> IntValue(value.toInt())
        else -> defaultIntegerValue(value)
    }
}

private fun getReceiverExpressionType(resolvedCall: ResolvedCall<*>): JetType? {
    return when (resolvedCall.getExplicitReceiverKind()) {
        ExplicitReceiverKind.THIS_OBJECT -> resolvedCall.getThisObject().getType()
        ExplicitReceiverKind.RECEIVER_ARGUMENT -> resolvedCall.getReceiverArgument().getType()
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

private val BYTE = CompileTimeType<Byte>()
private val SHORT = CompileTimeType<Short>()
private val INT = CompileTimeType<Int>()
private val LONG = CompileTimeType<Long>()
private val DOUBLE = CompileTimeType<Double>()
private val FLOAT = CompileTimeType<Float>()
private val CHAR = CompileTimeType<Char>()
private val BOOLEAN = CompileTimeType<Boolean>()
private val STRING = CompileTimeType<String>()
private val ANY = CompileTimeType<Any>()

[suppress("UNCHECKED_CAST")]
private fun <A, B> binaryOperationKey(
        a: CompileTimeType<A>,
        b: CompileTimeType<B>,
        functionName: String,
        f: (A, B) -> Any
) = BinaryOperationKey(a, b, functionName) to f as Function2<Any?, Any?, Any>

[suppress("UNCHECKED_CAST")]
private fun <A> unaryOperationKey(
        a: CompileTimeType<A>,
        functionName: String,
        f: (A) -> Any
) = UnaryOperationKey(a, functionName) to f  as Function1<Any?, Any>

private data class BinaryOperationKey<A, B>(val f: CompileTimeType<out A>, val s: CompileTimeType<out B>, val functionName: String)
private data class UnaryOperationKey<A>(val f: CompileTimeType<out A>, val functionName: String)
