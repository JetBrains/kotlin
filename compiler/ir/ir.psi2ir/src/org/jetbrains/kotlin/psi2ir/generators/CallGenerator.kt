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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(parentGenerator: StatementGenerator) : IrChildBodyGeneratorBase<StatementGenerator>(parentGenerator) {
    fun generateCall(
            ktElement: KtElement,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ): IrExpression {
        val descriptor = resolvedCall.resultingDescriptor

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, ktElement, resolvedCall)
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, ktElement, operator, resolvedCall, superQualifier)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    private fun CallGenerator.generatePropertyGetterCall(
            descriptor: PropertyDescriptor,
            ktElement: KtElement,
            resolvedCall: ResolvedCall<*>
    ): IrGetterCallImpl {
        val returnType = getReturnType(resolvedCall)
        val dispatchReceiver = generateReceiver(ktElement, resolvedCall.dispatchReceiver, descriptor.dispatchReceiverParameter)
        val extensionReceiver = generateReceiver(ktElement, resolvedCall.extensionReceiver, descriptor.extensionReceiverParameter)
        return IrGetterCallImpl(ktElement.startOffset, ktElement.endOffset,
                                returnType, descriptor.getter!!, resolvedCall.call.isSafeCall(),
                                dispatchReceiver, extensionReceiver, IrOperator.GET_PROPERTY)
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
            operator: IrOperator?,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            superQualifier: ClassDescriptor?
    ): IrExpression {
        val returnType = descriptor.returnType

        val irCall = IrCallImpl(
                ktElement.startOffset, ktElement.endOffset, returnType,
                descriptor, resolvedCall.call.isSafeCall(), operator, superQualifier
        )
        irCall.dispatchReceiver = generateReceiver(ktElement, resolvedCall.dispatchReceiver, descriptor.dispatchReceiverParameter)
        irCall.extensionReceiver = generateReceiver(ktElement, resolvedCall.extensionReceiver, descriptor.extensionReceiverParameter)

        return if (resolvedCall.requiresArgumentReordering()) {
            generateCallWithArgumentReordering(irCall, ktElement, resolvedCall, returnType)
        }
        else {
            val valueArguments = resolvedCall.valueArgumentsByIndex
            for (index in valueArguments!!.indices) {
                val valueArgument = valueArguments[index]
                val valueParameter = descriptor.valueParameters[index]
                val irArgument = generateValueArgument(valueArgument, valueParameter) ?: continue
                irCall.putArgument(index, irArgument)
            }
            irCall
        }
    }

    private fun generateCallWithArgumentReordering(
            irCall: IrCall,
            ktElement: KtElement,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            resultType: KotlinType?
    ): IrExpression {
        // TODO use IrLetExpression?

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters

        val hasResult = isUsedAsExpression(ktElement)
        val irBlock = IrBlockImpl(ktElement.startOffset, ktElement.endOffset, resultType, hasResult,
                                  IrOperator.SYNTHETIC_BLOCK)

        val valueArgumentsToValueParameters = HashMap<ResolvedValueArgument, ValueParameterDescriptor>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            valueArgumentsToValueParameters[valueArgument] = valueParameter
        }

        val reorderingScope = Scope(this.scope)

        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
            val irArgument = generateValueArgument(valueArgument, valueParameter) ?: continue
            val irTmpArg = reorderingScope.introduceTemporary(valueParameter, irArgument)
            irBlock.addIfNotNull(irTmpArg)
        }

        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            val irGetTemporary = reorderingScope.valueOf(valueParameter)!!
            irCall.putArgument(index, toExpectedType(irGetTemporary, valueParameter.type))
        }

        irBlock.addStatement(irCall)

        return irBlock
    }

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?, receiverParameterDescriptor: ReceiverParameterDescriptor?) =
            generateReceiver(ktElement, receiver, receiverParameterDescriptor?.type)

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?, expectedType: KotlinType?) =
            toExpectedTypeOrNull(generateReceiver(ktElement, receiver), expectedType)

    fun generateReceiver(ktElement: KtElement, receiver: ReceiverValue?): IrExpression? =
            if (receiver == null)
                null
            else
                scope.valueOf(receiver) ?: doGenerateReceiver(ktElement, receiver)

    fun doGenerateReceiver(ktElement: KtElement, receiver: ReceiverValue?): IrExpression? =
            when (receiver) {
                is ImplicitClassReceiver ->
                    IrThisReferenceImpl(ktElement.startOffset, ktElement.startOffset, receiver.type, receiver.classDescriptor)
                is ThisClassReceiver ->
                    (receiver as? ExpressionReceiver)?.expression?.let { receiverExpression ->
                        IrThisReferenceImpl(receiverExpression.startOffset, receiverExpression.endOffset, receiver.type, receiver.classDescriptor)
                    } ?: TODO("Non-implicit ThisClassReceiver should be an expression receiver")
                is ExpressionReceiver ->
                    generateExpression(receiver.expression)
                is ClassValueReceiver ->
                    IrGetObjectValueImpl(receiver.expression.startOffset, receiver.expression.endOffset, receiver.type,
                                         receiver.classQualifier.descriptor)
                is ExtensionReceiver ->
                    IrGetExtensionReceiverImpl(ktElement.startOffset, ktElement.startOffset, receiver.type,
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
                toExpectedTypeOrNull(doGenerateValueArgument(valueArgument, valueParameterDescriptor), expectedType)

    private fun doGenerateValueArgument(valueArgument: ResolvedValueArgument, valueParameterDescriptor: ValueParameterDescriptor): IrExpression? =
            if (valueArgument is DefaultValueArgument)
                null
            else
                scope.valueOf(valueParameterDescriptor) ?: doGenerateValueArgument(valueArgument)

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
            scope.valueOf(ktExpression) ?: parentGenerator.generateExpression(ktExpression)

}
