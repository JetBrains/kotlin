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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class CallGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateCall(startOffset: Int, endOffset: Int, call: CallBuilder, origin: IrStatementOrigin? = null): IrExpression {
        val descriptor = call.descriptor

        if (context.extensions.samConversion.isSamConstructor(descriptor.original)) {
            return generateSamConstructorCall(descriptor, startOffset, endOffset, call)
        }

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

    private fun generateSamConstructorCall(
        descriptor: CallableDescriptor,
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder
    ): IrExpression {
        val targetType = descriptor.returnType!!.toIrType()

        return IrTypeOperatorCallImpl(
            startOffset, endOffset,
            targetType,
            IrTypeOperator.SAM_CONVERSION,
            targetType,
            targetType.classifierOrFail,
            call.irValueArgumentsByIndex[0]!!
        )
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
                val fieldType = descriptor.propertyDescriptor.type.toIrType()
                IrGetFieldImpl(startOffset, endOffset, field, fieldType, receiver?.load())
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
                startOffset, endOffset, descriptor.type.toIrType(), getterSymbol, getterDescriptor,
                origin ?: IrStatementOrigin.GET_LOCAL_PROPERTY
            ).apply {
                putTypeArguments(typeArguments) { it.toIrType() }
            }
        } else
            IrGetValueImpl(startOffset, endOffset, descriptor.type.toIrType(), context.symbolTable.referenceValue(descriptor), origin)

    fun generateDelegatingConstructorCall(startOffset: Int, endOffset: Int, call: CallBuilder): IrExpression =
        call.callReceiver.call { dispatchReceiver, extensionReceiver ->
            val descriptor = call.descriptor as? ClassConstructorDescriptor
                ?: throw AssertionError("Class constructor expected: ${call.descriptor}")
            val constructorSymbol = context.symbolTable.referenceConstructor(descriptor.original)
            val irCall = IrDelegatingConstructorCallImpl(
                startOffset, endOffset,
                context.irBuiltIns.unitType,
                constructorSymbol,
                descriptor
            ).apply {
                putTypeArguments(call.typeArguments) { it.toIrType() }
                this.dispatchReceiver = dispatchReceiver?.load()
                this.extensionReceiver = extensionReceiver?.load()
            }
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
            val irCall = IrEnumConstructorCallImpl(startOffset, endOffset, constructorDescriptor.returnType.toIrType(), constructorSymbol)
            addParametersToCall(startOffset, endOffset, call, irCall, constructorDescriptor.returnType)
        }
    }

    private fun generatePropertyGetterCall(
        descriptor: PropertyDescriptor,
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder
    ): IrExpression {
        val getMethodDescriptor = descriptor.unwrappedGetMethod
        val superQualifierSymbol = call.superQualifier?.let { context.symbolTable.referenceClass(it) }
        val irType = descriptor.type.toIrType()

        return if (getMethodDescriptor == null) {
            call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                val fieldSymbol = context.symbolTable.referenceField(descriptor.original)
                IrGetFieldImpl(
                    startOffset, endOffset,
                    fieldSymbol,
                    irType,
                    dispatchReceiverValue?.load(),
                    IrStatementOrigin.GET_PROPERTY,
                    superQualifierSymbol
                )
            }
        } else {
            call.callReceiver.adjustForCallee(getMethodDescriptor).call { dispatchReceiverValue, extensionReceiverValue ->
                if (descriptor.isDynamic()) {
                    val dispatchReceiver = getDynamicExpressionReceiver(dispatchReceiverValue, extensionReceiverValue, descriptor)

                    IrDynamicMemberExpressionImpl(
                        startOffset, endOffset,
                        irType,
                        descriptor.name.asString(),
                        dispatchReceiver
                    )
                } else {
                    val getterSymbol = context.symbolTable.referenceFunction(getMethodDescriptor.original)
                    IrGetterCallImpl(
                        startOffset, endOffset,
                        irType,
                        getterSymbol,
                        getMethodDescriptor,
                        descriptor.typeParametersCount,
                        dispatchReceiverValue?.load(),
                        extensionReceiverValue?.load(),
                        IrStatementOrigin.GET_PROPERTY,
                        superQualifierSymbol
                    ).apply {
                        putTypeArguments(call.typeArguments) { it.toIrType() }
                    }
                }
            }
        }
    }

    private fun getDynamicExpressionReceiver(
        dispatchReceiverValue: IntermediateValue?,
        extensionReceiverValue: IntermediateValue?,
        referencedDescriptor: DeclarationDescriptor
    ): IrExpression {
        val dispatchReceiver = dispatchReceiverValue?.load()
            ?: throw AssertionError("Dynamic member reference $referencedDescriptor should have a dispatch receiver")
        if (dispatchReceiver.type !is IrDynamicType) {
            throw AssertionError(
                "Dynamic member reference $referencedDescriptor should have a receiver of dynamic type: ${dispatchReceiver.render()}"
            )
        }

        if (extensionReceiverValue != null) {
            throw AssertionError("Dynamic member reference $referencedDescriptor should have no extension receiver")
        }

        return dispatchReceiver
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
            val irType = returnType.toIrType()

            if (functionDescriptor.isDynamic()) {
                fun makeDynamicOperatorExpression(operator: IrDynamicOperator) =
                    IrDynamicOperatorExpressionImpl(
                        startOffset, endOffset,
                        irType,
                        operator
                    )

                fun makeDynamicOperatorExpressionWithArguments(operator: IrDynamicOperator, dynamicReceiver: IrExpression) =
                    makeDynamicOperatorExpression(operator).apply {
                        receiver = dynamicReceiver
                        arguments.addAll(call.getValueArgumentsInParameterOrder().mapIndexed { index: Int, arg: IrExpression? ->
                            arg ?: throw AssertionError("No argument in dynamic call $functionDescriptor at position $index")
                        })
                    }

                val dynamicReceiver = getDynamicExpressionReceiver(dispatchReceiverValue, extensionReceiverValue, functionDescriptor)

                when {
                    call.original.isImplicitInvoke() ->
                        makeDynamicOperatorExpressionWithArguments(IrDynamicOperator.INVOKE, dynamicReceiver)
                    call.original.isImplicitGet() ->
                        makeDynamicOperatorExpressionWithArguments(IrDynamicOperator.ARRAY_ACCESS, dynamicReceiver)
                    call.original.isImplicitSet() ->
                        makeDynamicOperatorExpression(IrDynamicOperator.EQ).apply {
                            val args = call.getValueArgumentsInParameterOrder()
                            val arg0 = args[0]
                                ?: throw AssertionError("No index argument in dynamic array set: ${call.original.call.callElement.text}")
                            val arg1 = args[1]
                                ?: throw AssertionError("No value argument in dynamic array set: ${call.original.call.callElement.text}")
                            left =
                                makeDynamicOperatorExpression(IrDynamicOperator.ARRAY_ACCESS).apply {
                                    left = dynamicReceiver
                                    right = arg0
                                }
                            right = arg1
                        }
                    else ->
                        makeDynamicOperatorExpressionWithArguments(
                            IrDynamicOperator.INVOKE,
                            IrDynamicMemberExpressionImpl(
                                startOffset, endOffset, // TODO obtain more exact start/end offsets for explicit receiver expression
                                dynamicReceiver.type,
                                functionDescriptor.name.asString(),
                                dynamicReceiver
                            )
                        )
                }
            } else {
                val functionSymbol = context.symbolTable.referenceFunction(functionDescriptor.original)
                val superQualifierSymbol = call.superQualifier?.let { context.symbolTable.referenceClass(it) }
                val irCall = IrCallImpl(
                    startOffset, endOffset,
                    irType,
                    functionSymbol,
                    functionDescriptor,
                    origin,
                    superQualifierSymbol
                ).apply {
                    putTypeArguments(call.typeArguments) { it.toIrType() }
                    this.dispatchReceiver = dispatchReceiverValue?.load()
                    this.extensionReceiver = extensionReceiverValue?.load()
                }
                addParametersToCall(startOffset, endOffset, call, irCall, returnType)
            }
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

        val irBlock = IrBlockImpl(startOffset, endOffset, resultType.toIrType(), IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL)

        val valueArgumentsToValueParameters = HashMap<ResolvedValueArgument, ValueParameterDescriptor>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val valueParameter = valueParameters[index]
            valueArgumentsToValueParameters[valueArgument] = valueParameter
        }

        val irArgumentValues = HashMap<ValueParameterDescriptor, IntermediateValue>()

        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
            val irArgument = call.getValueArgument(valueParameter) ?: continue
            val irArgumentValue = scope.createTemporaryVariableInBlock(context, irArgument, irBlock, valueParameter.name.asString())
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
    generateCall(ktElement.startOffsetSkippingComments, ktElement.endOffset, call, origin)

fun CallGenerator.generateCall(irExpression: IrExpression, call: CallBuilder, origin: IrStatementOrigin? = null) =
    generateCall(irExpression.startOffset, irExpression.endOffset, call, origin)
