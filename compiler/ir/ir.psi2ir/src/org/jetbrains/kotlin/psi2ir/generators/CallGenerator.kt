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
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(statementGenerator: StatementGenerator): StatementGeneratorExtension(statementGenerator) {
    fun generateCall(startOffset: Int, endOffset: Int, call: CallBuilder, origin: IrStatementOrigin? = null): IrExpression {
        val descriptor = call.descriptor

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, startOffset, endOffset, call)
            is VariableDescriptor ->
                call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                    generateGetVariable(startOffset, endOffset, descriptor, getTypeArguments(call.original), origin)
                }
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, startOffset, endOffset, origin, call)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    fun generateGetVariable(startOffset: Int, endOffset: Int,
                            descriptor: VariableDescriptor,
                            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
                            origin: IrStatementOrigin? = null) =
            @Suppress("DEPRECATION")
            if (descriptor is LocalVariableDescriptor && descriptor.isDelegated)
                IrCallImpl(startOffset, endOffset, descriptor.type, descriptor.getter!!, typeArguments, origin ?: IrStatementOrigin.GET_LOCAL_PROPERTY)
            else
                IrGetValueImpl(startOffset, endOffset, descriptor, origin)

    fun generateDelegatingConstructorCall(startOffset: Int, endOffset: Int, call: CallBuilder) : IrExpression {
        val descriptor = call.descriptor
        if (descriptor !is ClassConstructorDescriptor) throw AssertionError("Class constructor expected: $descriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            val irCall = IrDelegatingConstructorCallImpl(startOffset, endOffset, descriptor, getTypeArguments(call.original))
            irCall.dispatchReceiver = dispatchReceiver?.load()
            irCall.extensionReceiver = extensionReceiver?.load()
            addParametersToCall(startOffset, endOffset, call, irCall, descriptor.builtIns.unitType)
        }
    }

    fun generateEnumConstructorSuperCall(startOffset: Int, endOffset: Int, call: CallBuilder) : IrExpression {
        val constructorDescriptor = call.descriptor
        if (constructorDescriptor !is ClassConstructorDescriptor) throw AssertionError("Constructor expected: $constructorDescriptor")
        val classDescriptor = constructorDescriptor.containingDeclaration
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) throw AssertionError("Enum class constructor expected: $classDescriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            if (dispatchReceiver != null) throw AssertionError("Dispatch receiver should be null: $dispatchReceiver")
            if (extensionReceiver != null) throw AssertionError("Extension receiver should be null: $extensionReceiver")
            val irCall = IrEnumConstructorCallImpl(startOffset, endOffset, constructorDescriptor)
            addParametersToCall(startOffset, endOffset, call, irCall, constructorDescriptor.returnType)
        }
    }

    private fun generatePropertyGetterCall(
            descriptor: PropertyDescriptor,
            startOffset: Int,
            endOffset: Int,
            call: CallBuilder
    ): IrExpression {
        return call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            descriptor.getter?.let { getter ->
                IrGetterCallImpl(startOffset, endOffset, getter,
                                 getTypeArguments(call.original),
                                 dispatchReceiverValue?.load(),
                                 extensionReceiverValue?.load(),
                                 IrStatementOrigin.GET_PROPERTY,
                                 call.superQualifier)
            } ?: IrGetFieldImpl(startOffset, endOffset, descriptor,
                                dispatchReceiverValue?.load(),
                                IrStatementOrigin.GET_PROPERTY, call.superQualifier)
        }
    }

    private fun generateFunctionCall(
            descriptor: FunctionDescriptor,
            startOffset: Int,
            endOffset: Int,
            origin: IrStatementOrigin?,
            call: CallBuilder
    ): IrExpression {
        val returnType = descriptor.returnType!!

        return call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            val irCall = IrCallImpl(startOffset, endOffset, returnType, descriptor, getTypeArguments(call.original), origin, call.superQualifier)
            irCall.dispatchReceiver = dispatchReceiverValue?.load()
            irCall.extensionReceiver = extensionReceiverValue?.load()

            addParametersToCall(startOffset, endOffset, call, irCall, returnType)
        }
    }

    private fun addParametersToCall(startOffset: Int, endOffset: Int, call: CallBuilder, irCall: IrCallWithIndexedArgumentsBase, returnType: KotlinType): IrExpression =
            if (call.isValueArgumentReorderingRequired()) {
                generateCallWithArgumentReordering(irCall, startOffset, endOffset, call, returnType)
            }
            else {
                val valueArguments = call.getValueArgumentsInParameterOrder()
                for ((index, valueArgument) in valueArguments.withIndex()) {
                    irCall.putValueArgument(index, valueArgument)
                }
                irCall
            }

    private fun generateCallWithArgumentReordering(
            irCall: IrMemberAccessExpression,
            startOffset: Int,
            endOffset: Int,
            call: CallBuilder,
            resultType: KotlinType
    ): IrExpression {
        val resolvedCall = call.original

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters

        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL)

        val valueArgumentsToValueParameters = HashMap<ResolvedValueArgument, ValueParameterDescriptor>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            valueArgumentsToValueParameters[valueArgument] = valueParameter
        }

        val irArgumentValues = HashMap<ValueParameterDescriptor, IntermediateValue>()

        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
            val irArgument = call.getValueArgument(valueParameter) ?: continue
            val irArgumentValue = scope.createTemporaryVariableInBlock(irArgument, irBlock, valueParameter.name.asString())
            irArgumentValues[valueParameter] = irArgumentValue
        }

        resolvedCall.valueArgumentsByIndex!!.forEachIndexed { index, valueArgument ->
            val valueParameter = valueParameters[index]
            irCall.putValueArgument(index, irArgumentValues[valueParameter]?.load())
        }

        irBlock.statements.add(irCall)

        return irBlock
    }
}

fun CallGenerator.generateCall(ktElement: KtElement, call: CallBuilder, origin: IrStatementOrigin? = null) =
        generateCall(ktElement.startOffset, ktElement.endOffset, call, origin)

fun CallGenerator.generateCall(irExpression: IrExpression, call: CallBuilder, origin: IrStatementOrigin? = null) =
        generateCall(irExpression.startOffset, irExpression.endOffset, call, origin)
