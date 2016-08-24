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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.PregeneratedCall
import org.jetbrains.kotlin.psi2ir.intermediate.getValueArgumentsInParameterOrder
import org.jetbrains.kotlin.psi2ir.intermediate.isValueArgumentReorderingRequired
import org.jetbrains.kotlin.psi2ir.intermediate.IntermediateValue
import org.jetbrains.kotlin.psi2ir.intermediate.createRematerializableOrTemporary
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(
        override val context: GeneratorContext,
        override val scope: Scope
) : GeneratorWithScope {

    constructor(parent: GeneratorWithScope) : this(parent.context, parent.scope)

    fun generateCall(
            startOffset: Int,
            endOffset: Int,
            call: PregeneratedCall,
            operator: IrOperator? = null
    ): IrExpression {
        val descriptor = call.descriptor

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, startOffset, endOffset, call)
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, startOffset, endOffset, operator, call)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    private fun generatePropertyGetterCall(
            descriptor: PropertyDescriptor,
            startOffset: Int,
            endOffset: Int,
            call: PregeneratedCall
    ): IrExpression {
        return call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            IrGetterCallImpl(startOffset, endOffset, descriptor.getter!!,
                             dispatchReceiverValue?.load(),
                             extensionReceiverValue?.load(),
                             IrOperator.GET_PROPERTY)
        }
    }

    private fun generateFunctionCall(
            descriptor: FunctionDescriptor,
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator?,
            call: PregeneratedCall
    ): IrExpression {
        val returnType = descriptor.returnType

        return call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            val irCall = IrCallImpl(startOffset, endOffset, returnType, descriptor, operator, call.superQualifier)
            irCall.dispatchReceiver = dispatchReceiverValue?.load()
            irCall.extensionReceiver = extensionReceiverValue?.load()

            val irCallWithReordering =
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

            irCallWithReordering
        }
    }

    private fun generateCallWithArgumentReordering(
            irCall: IrCall,
            startOffset: Int,
            endOffset: Int,
            call: PregeneratedCall,
            resultType: KotlinType?
    ): IrExpression {
        val resolvedCall = call.original

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters

        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, true, IrOperator.SYNTHETIC_BLOCK)

        val valueArgumentsToValueParameters = HashMap<ResolvedValueArgument, ValueParameterDescriptor>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            valueArgumentsToValueParameters[valueArgument] = valueParameter
        }

        val irArgumentValues = HashMap<ValueParameterDescriptor, IntermediateValue>()

        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
            val irArgument = call.getValueArgument(valueParameter) ?: continue
            val irArgumentValue = createRematerializableOrTemporary(scope, irArgument, irBlock, valueParameter.name.asString())
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

fun CallGenerator.generateCall(ktElement: KtElement, call: PregeneratedCall, operator: IrOperator? = null) =
        generateCall(ktElement.startOffset, ktElement.endOffset, call, operator)

fun CallGenerator.generateCall(irExpression: IrExpression, call: PregeneratedCall, operator: IrOperator? = null) =
        generateCall(irExpression.startOffset, irExpression.endOffset, call, operator)
