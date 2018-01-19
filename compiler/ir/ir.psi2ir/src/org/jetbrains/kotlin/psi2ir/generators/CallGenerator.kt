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
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateCall(startOffset: Int, endOffset: Int, call: CallBuilder, origin: IrStatementOrigin? = null): IrExpression {
        val descriptor = call.descriptor

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, startOffset, endOffset, call)
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, startOffset, endOffset, origin, call)
            else ->
                call.callReceiver.call { _, _ ->
                    generateValueReference(startOffset, endOffset, descriptor, call.original, origin)
                }
        }
    }

    fun generateValueReference(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor,
        resolvedCall: ResolvedCall<*>?,
        origin: IrStatementOrigin?
    ): IrExpression =
        when (descriptor) {
            is FakeCallableDescriptorForObject ->
                generateValueReference(startOffset, endOffset, descriptor.getReferencedDescriptor(), resolvedCall, origin)
            is TypeAliasDescriptor ->
                generateValueReference(startOffset, endOffset, descriptor.classDescriptor!!, null, origin)
            is ClassDescriptor -> {
                val classValueType = descriptor.classValueType!!
                statementGenerator.generateSingletonReference(descriptor, startOffset, endOffset, classValueType)
            }
            is PropertyDescriptor -> {
                generateCall(startOffset, endOffset, statementGenerator.pregenerateCall(resolvedCall!!))
            }
            is SyntheticFieldDescriptor -> {
                val receiver = statementGenerator.generateBackingFieldReceiver(startOffset, endOffset, resolvedCall, descriptor)
                val field = statementGenerator.context.symbolTable.referenceField(descriptor.propertyDescriptor)
                IrGetFieldImpl(startOffset, endOffset, field, receiver?.load())
            }
            is VariableDescriptor ->
                generateGetVariable(startOffset, endOffset, descriptor, getTypeArguments(resolvedCall), origin)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor::class.java.simpleName}")
        }

    private fun generateGetVariable(
        startOffset: Int,
        endOffset: Int,
        descriptor: VariableDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin? = null
    ) =
        @Suppress("DEPRECATION")
        if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
            val getterDescriptor = descriptor.getter!!
            val getterSymbol = context.symbolTable.referenceFunction(getterDescriptor.original)
            IrCallImpl(
                startOffset, endOffset, descriptor.type, getterSymbol, getterDescriptor,
                typeArguments, origin ?: IrStatementOrigin.GET_LOCAL_PROPERTY
            )
        } else
            IrGetValueImpl(startOffset, endOffset, context.symbolTable.referenceValue(descriptor), origin)

    fun generateDelegatingConstructorCall(startOffset: Int, endOffset: Int, call: CallBuilder): IrExpression =
        call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            val descriptor = call.descriptor as? ClassConstructorDescriptor
                    ?: throw AssertionError("Class constructor expected: ${call.descriptor}")
            val constructorSymbol = context.symbolTable.referenceConstructor(descriptor.original)
            val irCall =
                IrDelegatingConstructorCallImpl(startOffset, endOffset, constructorSymbol, descriptor, getTypeArguments(call.original))
            irCall.dispatchReceiver = dispatchReceiver?.load()
            irCall.extensionReceiver = extensionReceiver?.load()
            addParametersToCall(startOffset, endOffset, call, irCall, descriptor.builtIns.unitType)
        }

    fun generateEnumConstructorSuperCall(startOffset: Int, endOffset: Int, call: CallBuilder): IrExpression {
        val constructorDescriptor = call.descriptor
        if (constructorDescriptor !is ClassConstructorDescriptor) throw AssertionError("Constructor expected: $constructorDescriptor")
        val classDescriptor = constructorDescriptor.containingDeclaration
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) throw AssertionError("Enum class constructor expected: $classDescriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            if (dispatchReceiver != null) throw AssertionError("Dispatch receiver should be null: $dispatchReceiver")
            if (extensionReceiver != null) throw AssertionError("Extension receiver should be null: $extensionReceiver")
            val constructorSymbol = context.symbolTable.referenceConstructor(constructorDescriptor.original)
            val irCall = IrEnumConstructorCallImpl(startOffset, endOffset, constructorSymbol)
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
            val superQualifierSymbol = call.superQualifier?.let { context.symbolTable.referenceClass(it) }

            val getterDescriptor = descriptor.getter
            if (getterDescriptor != null) {
                val getterSymbol = context.symbolTable.referenceFunction(getterDescriptor.original)
                IrGetterCallImpl(
                    startOffset, endOffset,
                    getterSymbol,
                    getterDescriptor,
                    getTypeArguments(call.original),
                    dispatchReceiverValue?.load(),
                    extensionReceiverValue?.load(),
                    IrStatementOrigin.GET_PROPERTY,
                    superQualifierSymbol
                )
            } else {
                val fieldSymbol = context.symbolTable.referenceField(descriptor)
                IrGetFieldImpl(
                    startOffset, endOffset,
                    fieldSymbol,
                    dispatchReceiverValue?.load(),
                    IrStatementOrigin.GET_PROPERTY,
                    superQualifierSymbol
                )
            }
        }
    }

    private fun generateFunctionCall(
        functionDescriptor: FunctionDescriptor,
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        call: CallBuilder
    ): IrExpression =
        call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            val returnType = functionDescriptor.returnType!!
            val functionSymbol = context.symbolTable.referenceFunction(functionDescriptor.original)
            val superQualifierSymbol = call.superQualifier?.let { context.symbolTable.referenceClass(it) }
            val irCall = IrCallImpl(
                startOffset, endOffset,
                returnType,
                functionSymbol,
                functionDescriptor,
                getTypeArguments(call.original),
                origin,
                superQualifierSymbol
            )
            irCall.dispatchReceiver = dispatchReceiverValue?.load()
            irCall.extensionReceiver = extensionReceiverValue?.load()

            addParametersToCall(startOffset, endOffset, call, irCall, returnType)
        }

    private fun addParametersToCall(
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder,
        irCall: IrFunctionAccessExpression,
        returnType: KotlinType
    ): IrExpression =
        if (call.isValueArgumentReorderingRequired()) {
            generateCallWithArgumentReordering(irCall, startOffset, endOffset, call, returnType)
        } else {
            val valueArguments = call.getValueArgumentsInParameterOrder()
            for ((index, valueArgument) in valueArguments.withIndex()) {
                irCall.putValueArgument(index, valueArgument)
            }
            irCall
        }

    private fun generateCallWithArgumentReordering(
        irCall: IrFunctionAccessExpression,
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

        resolvedCall.valueArgumentsByIndex!!.forEachIndexed { index, _ ->
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
