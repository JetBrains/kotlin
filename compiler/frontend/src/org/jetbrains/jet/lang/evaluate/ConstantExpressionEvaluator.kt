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
import java.lang.Short as JShort
import java.lang.Byte as JByte

[suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
public class ConstantExpressionEvaluator private (val trace: BindingTrace) : JetVisitor<CompileTimeConstant<*>, JetType>() {

    public fun evaluate(expression: JetExpression, expectedType: JetType?): CompileTimeConstant<*>? {
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
        return trace.get(BindingContext.COMPILE_TIME_VALUE, expression)
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
        } else {
            super.visitPrefixExpression(expression, expectedType)
        }
    }

    override fun visitStringTemplateExpression(expression: JetStringTemplateExpression, expectedType: JetType?): CompileTimeConstant<*>? {
        return with(StringBuilder()) {
            var interupted = false
            for (entry in expression.getEntries()) {
                val constant = stringExpressionEvaluator.evaluate(entry)
                if (constant == null) {
                    interupted = true
                    break
                }
                else {
                    append(constant.getValue())
                }
            }
            if (!interupted) createCompileTimeConstant(toString(), expectedType) else null
        }
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

            val operationName = when(operationToken) {
                JetTokens.ANDAND -> Name.identifier("&&")
                JetTokens.OROR -> Name.identifier("||")
                else -> throw IllegalArgumentException("Unknown boolean operation token ${operationToken}")
            }
            val result = evaluateBinaryExpression(leftConstant, rightConstant, operationName)
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

        val resultingDescriptor = resolvedCall.getResultingDescriptor()
        if (resultingDescriptor == null) return null

        val receiverExpressionType = getReceiverExpressionType(resolvedCall)
        if (receiverExpressionType == null) return null

        val receiverValue = evaluate(receiverExpression, receiverExpressionType)
        if (receiverValue == null) return null

        val arguments = resolvedCall.getValueArguments().entrySet().flatMap {
            entry ->
            val (parameter, argument) = entry
            resolveArguments(argument.getArguments(), parameter.getType())
        }

        val resultingDescriptorName = resultingDescriptor.getName()
        if (arguments.isEmpty()) {
            return evaluateUnaryExpression(receiverValue, resultingDescriptorName)
        }
        else if (arguments.size() == 1) {
            return evaluateBinaryExpression(receiverValue, arguments.first(), resultingDescriptorName)
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
        //todo flatMap
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

    class object {
        public fun evaluate(expression: JetExpression, trace: BindingTrace, expectedType: JetType? = TypeUtils.NO_EXPECTED_TYPE): CompileTimeConstant<*>? {
            val evaluator = ConstantExpressionEvaluator(trace)
            return evaluator.evaluate(expression, expectedType)
        }
    }
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
        else -> StringValue(value.getValue().toString())
    }
}

private fun createCompileTimeConstant(value: Any?, expectedType: JetType?): CompileTimeConstant<*>? {
    return when(value) {
        null -> null
        is Byte, is Short, is Int, is Long -> getIntegerValue((value as Number).toLong(), expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        is Char -> CharValue(value)
        is Float -> if (CompileTimeConstantResolver.noExpectedTypeOrError(expectedType) ||
        expectedType == KotlinBuiltIns.getInstance().getDoubleType()) DoubleValue(value.toDouble())
        else FloatValue(value)
        is Double -> DoubleValue(value)
        is Boolean -> if (value) BooleanValue.TRUE else BooleanValue.FALSE
        is String -> StringValue(value)
        else -> null
    }
}
private fun getIntegerValue(value: Long, expectedType: JetType): CompileTimeConstant<*>? {
    if (CompileTimeConstantResolver.noExpectedTypeOrError(expectedType)) {
        if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
            return IntValue(value.toInt())
        }

        return LongValue(value)
    }

    fun defaultIntegerValue(value: Long) = when (value) {
        in Integer.MIN_VALUE..Integer.MAX_VALUE.toLong() -> IntValue(value.toInt())
        else -> LongValue(value)
    }

    val builtIns = KotlinBuiltIns.getInstance()
    return when (TypeUtils.makeNotNullable(expectedType)) {
        builtIns.getIntType() -> IntValue(value.toInt())
        builtIns.getLongType() -> LongValue(value)
        builtIns.getShortType() -> when (value) {
            in JShort.MIN_VALUE..JShort.MAX_VALUE.toLong() -> ShortValue(value.toShort())
            else -> defaultIntegerValue(value)
        }
        builtIns.getByteType() -> when (value) {
            in JByte.MIN_VALUE..JByte.MAX_VALUE.toLong() -> ByteValue(value.toByte())
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

