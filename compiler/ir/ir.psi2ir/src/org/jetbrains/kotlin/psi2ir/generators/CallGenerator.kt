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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(statementGenerator: StatementGenerator): StatementGeneratorExtension(statementGenerator) {
    fun generateCall(startOffset: Int, endOffset: Int, call: CallBuilder, operator: IrOperator? = null): IrExpression {
        val descriptor = call.descriptor

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, startOffset, endOffset, call)
            is VariableDescriptor ->
                call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                    generateGetVariable(startOffset, endOffset, descriptor, operator)
                }
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, startOffset, endOffset, operator, call)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    fun generateGetVariable(startOffset: Int, endOffset: Int, descriptor: VariableDescriptor, operator: IrOperator? = null) =
            if (descriptor is LocalVariableDescriptor && descriptor.isDelegated)
                IrCallImpl(startOffset, endOffset, descriptor.type, descriptor.getter!!, operator ?: IrOperator.GET_LOCAL_PROPERTY)
            else
                IrGetVariableImpl(startOffset, endOffset, descriptor, operator)

    fun generateDelegatingConstructorCall(startOffset: Int, endOffset: Int, call: CallBuilder) : IrExpression {
        val descriptor = call.descriptor
        if (descriptor !is ConstructorDescriptor) throw AssertionError("Constructor expected: $descriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            val irCall = IrDelegatingConstructorCallImpl(startOffset, endOffset, descriptor)
            irCall.dispatchReceiver = dispatchReceiver?.load()
            irCall.extensionReceiver = extensionReceiver?.load()
            addParametersToCall(startOffset, endOffset, call, irCall, descriptor.returnType)
        }
    }

    fun generateEnumConstructorSuperCall(startOffset: Int, endOffset: Int, call: CallBuilder,
                                         enumEntryDescriptor: ClassDescriptor?) : IrExpression {
        val constructorDescriptor = call.descriptor
        if (constructorDescriptor !is ConstructorDescriptor) throw AssertionError("Constructor expected: $constructorDescriptor")
        val classDescriptor = constructorDescriptor.containingDeclaration
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) throw AssertionError("Enum class constructor expected: $classDescriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            if (dispatchReceiver != null) throw AssertionError("Dispatch receiver should be null: $dispatchReceiver")
            if (extensionReceiver != null) throw AssertionError("Extension receiver should be null: $extensionReceiver")
            val irCall = IrEnumConstructorCallImpl(startOffset, endOffset, constructorDescriptor, enumEntryDescriptor)
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
                                 dispatchReceiverValue?.load(),
                                 extensionReceiverValue?.load(),
                                 IrOperator.GET_PROPERTY,
                                 call.superQualifier)
            } ?: IrGetBackingFieldImpl(startOffset, endOffset, descriptor,
                                       dispatchReceiverValue?.load(),
                                       IrOperator.GET_PROPERTY, call.superQualifier)
        }
    }

    private fun generateFunctionCall(
            descriptor: FunctionDescriptor,
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator?,
            call: CallBuilder
    ): IrExpression {
        val returnType = descriptor.returnType!!

        return call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            val irCall = IrCallImpl(startOffset, endOffset, returnType, descriptor, operator, call.superQualifier)
            irCall.dispatchReceiver = dispatchReceiverValue?.load()
            irCall.extensionReceiver = extensionReceiverValue?.load()

            addParametersToCall(startOffset, endOffset, call, irCall, returnType)
        }
    }

    private fun addParametersToCall(startOffset: Int, endOffset: Int, call: CallBuilder, irCall: IrGeneralCallBase, returnType: KotlinType): IrExpression =
            if (call.isValueArgumentReorderingRequired()) {
                generateCallWithArgumentReordering(irCall, startOffset, endOffset, call, returnType)
            }
            else {
                val valueArguments = call.getValueArgumentsInParameterOrder()
                for ((index, valueArgument) in valueArguments.withIndex()) {
                    irCall.putArgument(index, valueArgument)
                }
                irCall
            }

    private fun generateCallWithArgumentReordering(
            irCall: IrGeneralCall,
            startOffset: Int,
            endOffset: Int,
            call: CallBuilder,
            resultType: KotlinType
    ): IrExpression {
        val resolvedCall = call.original

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters

        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, IrOperator.ARGUMENTS_REORDERING_FOR_CALL)

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
            irCall.putArgument(index, irArgumentValues[valueParameter]?.load())
        }

        irBlock.addStatement(irCall)

        return irBlock
    }
}

fun CallGenerator.generateCall(ktElement: KtElement, call: CallBuilder, operator: IrOperator? = null) =
        generateCall(ktElement.startOffset, ktElement.endOffset, call, operator)

fun CallGenerator.generateCall(irExpression: IrExpression, call: CallBuilder, operator: IrOperator? = null) =
        generateCall(irExpression.startOffset, irExpression.endOffset, call, operator)
