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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.values.VariableLValue
import org.jetbrains.kotlin.psi2ir.generators.values.IrValue
import org.jetbrains.kotlin.psi2ir.generators.values.createRematerializableValue
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(val statementGenerator: StatementGenerator) : IrGenerator {
    override val context: GeneratorContext get() =
            statementGenerator.context

    private val temporaryVariableFactory: TemporaryVariableFactory get() =
            statementGenerator.temporaryVariableFactory

    private val expressionValues = HashMap<KtExpression, IrValue>()
    private val receiverValues = HashMap<ReceiverValue, IrValue>()
    private val valueArgumentValues = HashMap<ValueParameterDescriptor, IrValue>()

    fun introduceTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String? = null): IrVariable? {
        val rematerializable = createRematerializableValue(irExpression)
        if (rematerializable != null) {
            putValue(ktExpression, rematerializable)
            return null
        }

        return createTemporary(ktExpression, irExpression, nameHint)
    }

    fun createTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String?): IrVariable {
        val irTmpVar = temporaryVariableFactory.createTemporaryVariable(irExpression, nameHint)
        putValue(ktExpression, VariableLValue(irTmpVar))
        return irTmpVar
    }

    fun introduceTemporary(valueParameterDescriptor: ValueParameterDescriptor, irExpression: IrExpression): IrVariable? {
        val rematerializable = createRematerializableValue(irExpression)
        if (rematerializable != null) {
            putValue(valueParameterDescriptor, rematerializable)
            return null
        }

        val irTmpVar = temporaryVariableFactory.createTemporaryVariable(irExpression, valueParameterDescriptor.name.asString())
        putValue(valueParameterDescriptor, VariableLValue(irTmpVar))
        return irTmpVar
    }

    fun putValue(ktExpression: KtExpression, irValue: IrValue) {
        expressionValues[ktExpression] = irValue
    }

    fun putValue(receiver: ReceiverValue, irValue: IrValue) {
        receiverValues[receiver] = irValue
    }

    fun putValue(parameter: ValueParameterDescriptor, irValue: IrValue) {
        valueArgumentValues[parameter] = irValue
    }

    fun valueOf(ktExpression: KtExpression) = expressionValues[ktExpression]?.load()
    fun valueOf(receiver: ReceiverValue) = receiverValues[receiver]?.load()
    fun valueOf(parameter: ValueParameterDescriptor) = valueArgumentValues[parameter]?.load()

    fun generateCall(
            ktElement: KtElement,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ): IrExpression {
        val descriptor = resolvedCall.resultingDescriptor
        val returnType = getReturnType(resolvedCall)

        return when (descriptor) {
            is PropertyDescriptor ->
                IrGetPropertyExpressionImpl(
                        ktElement.startOffset, ktElement.endOffset,
                        returnType,
                        resolvedCall.call.isSafeCall(), descriptor
                ).apply {
                    dispatchReceiver = generateReceiver(ktElement, resolvedCall.dispatchReceiver, descriptor.dispatchReceiverParameter)
                    extensionReceiver = generateReceiver(ktElement, resolvedCall.extensionReceiver, descriptor.extensionReceiverParameter)
                }
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, ktElement, returnType, operator, resolvedCall, superQualifier)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    private fun ResolvedCall<*>.requiresArgumentReordering(): Boolean {
        var lastValueParameterIndex = -1
        for (valueArgument in call.valueArguments) {
            val argumentMapping = getArgumentMapping(valueArgument)
            if (argumentMapping !is ArgumentMatch || argumentMapping.isError()) {
                error("Value argument in function call is mapped with error")
            }
            val argumentIndex = argumentMapping.valueParameter.index
            if (argumentIndex < lastValueParameterIndex) return true
            lastValueParameterIndex = argumentIndex
        }
        return false
    }

    private fun generateFunctionCall(
            descriptor: FunctionDescriptor,
            ktElement: KtElement,
            resultType: KotlinType?,
            operator: IrOperator?,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            superQualifier: ClassDescriptor?
    ): IrExpression {
        val irCall = IrCallExpressionImpl(
                ktElement.startOffset, ktElement.endOffset, resultType,
                descriptor, resolvedCall.call.isSafeCall(), operator, superQualifier
        )
        irCall.dispatchReceiver = generateReceiver(ktElement, resolvedCall.dispatchReceiver, descriptor.dispatchReceiverParameter)
        irCall.extensionReceiver = generateReceiver(ktElement, resolvedCall.extensionReceiver, descriptor.extensionReceiverParameter)

        return if (resolvedCall.requiresArgumentReordering()) {
            generateCallWithArgumentReordering(irCall, ktElement, resolvedCall, resultType)
        }
        else {
            irCall.apply {
                val valueArguments = resolvedCall.valueArgumentsByIndex
                for (index in valueArguments!!.indices) {
                    val valueArgument = valueArguments[index]
                    val valueParameter = descriptor.valueParameters[index]
                    val irArgument = generateValueArgument(valueArgument, valueParameter) ?: continue
                    irCall.putArgument(index, irArgument)
                }
            }
        }
    }

    private fun generateCallWithArgumentReordering(
            irCall: IrCallExpression,
            ktElement: KtElement,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            resultType: KotlinType?
    ): IrExpression {
        // TODO use IrLetExpression?

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters

        val hasResult = isUsedAsExpression(ktElement)
        val irBlock = IrBlockExpressionImpl(ktElement.startOffset, ktElement.endOffset, resultType, hasResult,
                                            IrOperator.SYNTHETIC_BLOCK)

        val valueArgumentsToValueParameters = HashMap<ResolvedValueArgument, ValueParameterDescriptor>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            valueArgumentsToValueParameters[valueArgument] = valueParameter
        }

        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
            val irArgument = generateValueArgument(valueArgument, valueParameter) ?: continue
            val irTmpArg = introduceTemporary(valueParameter, irArgument)
            irBlock.addIfNotNull(irTmpArg)
        }

        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            val irGetTemporary = valueArgumentValues[valueParameter]!!.load()
            irCall.putArgument(index, irGetTemporary.toExpectedType(valueParameter.type))
        }

        irBlock.addStatement(irCall)

        return irBlock
    }

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?, receiverParameterDescriptor: ReceiverParameterDescriptor?) =
            generateReceiver(ktElement, receiver, receiverParameterDescriptor?.type)

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?, expectedType: KotlinType?) =
            generateReceiver(ktElement, receiver)?.toExpectedType(expectedType)

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?): IrExpression? =
            if (receiver == null)
                null
            else
                receiverValues[receiver]?.load() ?: doGenerateReceiver(ktElement, receiver)

    fun doGenerateReceiver(ktElement: KtElement, receiver: ReceiverValue?): IrExpression? =
            when (receiver) {
                is ImplicitClassReceiver ->
                    IrThisExpressionImpl(ktElement.startOffset, ktElement.startOffset, receiver.type, receiver.classDescriptor)
                is ThisClassReceiver ->
                    (receiver as? ExpressionReceiver)?.expression?.let { receiverExpression ->
                        IrThisExpressionImpl(receiverExpression.startOffset, receiverExpression.endOffset, receiver.type, receiver.classDescriptor)
                    } ?: TODO("Non-implicit ThisClassReceiver should be an expression receiver")
                is ExpressionReceiver ->
                    generateExpression(receiver.expression)
                is ClassValueReceiver ->
                    IrGetObjectValueExpressionImpl(receiver.expression.startOffset, receiver.expression.endOffset, receiver.type,
                                                   receiver.classQualifier.descriptor)
                is ExtensionReceiver ->
                    IrGetExtensionReceiverExpressionImpl(ktElement.startOffset, ktElement.startOffset, receiver.type,
                                                         receiver.declarationDescriptor.extensionReceiverParameter!!)
                null ->
                    null
                else ->
                    TODO("Receiver: ${receiver.javaClass.simpleName}")
            }

    fun generateValueArgument(valueArgument: ResolvedValueArgument, valueParameterDescriptor: ValueParameterDescriptor): IrExpression? =
            generateValueArgument(valueArgument, valueParameterDescriptor, valueParameterDescriptor.type)

    fun generateValueArgument(valueArgument: ResolvedValueArgument, valueParameterDescriptor: ValueParameterDescriptor, expectedType: KotlinType): IrExpression? =
            if (valueParameterDescriptor.varargElementType != null)
                doGenerateValueArgument(valueArgument, valueParameterDescriptor)
            else
                doGenerateValueArgument(valueArgument, valueParameterDescriptor)?.toExpectedType(expectedType)

    private fun doGenerateValueArgument(valueArgument: ResolvedValueArgument, valueParameterDescriptor: ValueParameterDescriptor): IrExpression? =
            if (valueArgument is DefaultValueArgument)
                null
            else
                valueArgumentValues[valueParameterDescriptor]?.load() ?: doGenerateValueArgument(valueArgument)

    private fun doGenerateValueArgument(valueArgument: ResolvedValueArgument): IrExpression? =
            when (valueArgument) {
                is ExpressionValueArgument ->
                    generateExpression(valueArgument.valueArgument!!.getArgumentExpression()!!)
                is VarargValueArgument ->
                    createDummyExpression(valueArgument.arguments[0].getArgumentExpression()!!, "vararg")
                else ->
                    TODO("Unexpected valueArgument: ${valueArgument.javaClass.simpleName}")
            }

    private fun generateExpression(ktExpression: KtExpression): IrExpression =
            expressionValues[ktExpression]?.load() ?: statementGenerator.generateExpression(ktExpression)

}
