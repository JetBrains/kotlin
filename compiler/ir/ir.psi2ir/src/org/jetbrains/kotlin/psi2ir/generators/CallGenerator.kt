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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.psi2ir.resolveFakeOverride
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.types.KotlinType

internal class CallGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateCall(startOffset: Int, endOffset: Int, call: CallBuilder, origin: IrStatementOrigin? = null): IrExpression {
        val descriptor = call.descriptor

        if (descriptor.original.isSamConstructor()) {
            return generateSamConstructorCall(descriptor, startOffset, endOffset, call)
        }

        return when (descriptor) {
            is PropertyDescriptor ->
                generatePropertyGetterCall(descriptor, startOffset, endOffset, call)
            is ClassConstructorDescriptor ->
                generateConstructorCall(descriptor, startOffset, endOffset, origin, call)
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, startOffset, endOffset, origin, call)
            else ->
                call.callReceiver.call { _, _, _ ->
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
        val targetKotlinType = descriptor.returnType!!
        val targetType = targetKotlinType.toIrType()

        return IrTypeOperatorCallImpl(
            startOffset, endOffset,
            targetType,
            IrTypeOperator.SAM_CONVERSION,
            targetType,
            statementGenerator.castArgumentToFunctionalInterfaceForSamType(
                call.irValueArgumentsByIndex[0]!!,
                targetKotlinType
            )
        )
    }

    fun generateValueReference(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor,
        resolvedCall: ResolvedCall<*>?,
        origin: IrStatementOrigin?,
        smartCastIrType: IrType? = null
    ): IrExpression {
        context.fragmentContext?.capturedDescriptorToFragmentParameterMap?.get(descriptor)?.let {
            val getValue = IrGetValueImpl(startOffset, endOffset, it.descriptor.type.toIrType(), it, origin)
            return if (smartCastIrType != null) {
                IrTypeOperatorCallImpl(startOffset, endOffset, smartCastIrType, IrTypeOperator.IMPLICIT_CAST, smartCastIrType, getValue)
            } else {
                getValue
            }
        }
        return when (descriptor) {
            is FakeCallableDescriptorForObject ->
                generateValueReference(startOffset, endOffset, descriptor.getReferencedDescriptor(), resolvedCall, origin, smartCastIrType)
            is TypeAliasDescriptor ->
                generateValueReference(startOffset, endOffset, descriptor.classDescriptor!!, null, origin, smartCastIrType)
            is ClassDescriptor -> {
                val classValueType = descriptor.classValueType!!
                statementGenerator.generateSingletonReference(descriptor, startOffset, endOffset, classValueType)
            }
            is PropertyDescriptor -> {
                val irCall = generateCall(startOffset, endOffset, statementGenerator.pregenerateCall(resolvedCall!!))
                if (smartCastIrType != null)
                    IrTypeOperatorCallImpl(startOffset, endOffset, smartCastIrType, IrTypeOperator.IMPLICIT_CAST, smartCastIrType, irCall)
                else
                    irCall
            }
            is SyntheticFieldDescriptor -> {
                val receiver = statementGenerator.generateBackingFieldReceiver(startOffset, endOffset, resolvedCall, descriptor)
                val field = statementGenerator.context.symbolTable.descriptorExtension.referenceField(descriptor.propertyDescriptor)
                val fieldType = descriptor.propertyDescriptor.type.toIrType()
                IrGetFieldImpl(startOffset, endOffset, field, fieldType, receiver?.load())
            }
            is VariableDescriptor ->
                generateGetVariable(startOffset, endOffset, descriptor, getTypeArguments(resolvedCall), origin, smartCastIrType)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor::class.java.simpleName}")
        }
    }

    private fun generateGetVariable(
        startOffset: Int,
        endOffset: Int,
        descriptor: VariableDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin? = null,
        irType: IrType? = null
    ) =
        if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
            val getterDescriptor = descriptor.getter!!
            val getterSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(getterDescriptor.original)
            IrCallImpl.fromSymbolDescriptor(
                startOffset, endOffset, descriptor.type.toIrType(), getterSymbol, origin = origin ?: IrStatementOrigin.GET_LOCAL_PROPERTY
            ).apply {
                context.callToSubstitutedDescriptorMap[this] = getterDescriptor
                putTypeArguments(typeArguments) { it.toIrType() }
            }
        } else {
            val getValue =
                IrGetValueImpl(startOffset, endOffset, descriptor.type.toIrType(), context.symbolTable.descriptorExtension.referenceValue(descriptor), origin)
            if (irType != null) {
                IrTypeOperatorCallImpl(startOffset, endOffset, irType, IrTypeOperator.IMPLICIT_CAST, irType, getValue)
            } else {
                getValue
            }
        }

    fun generateDelegatingConstructorCall(startOffset: Int, endOffset: Int, call: CallBuilder): IrExpression =
        call.callReceiver.call { dispatchReceiver, extensionReceiver, contextReceivers ->
            val descriptor = call.descriptor as? ClassConstructorDescriptor
                ?: throw AssertionError("Class constructor expected: ${call.descriptor}")
            val constructorSymbol = context.symbolTable.descriptorExtension.referenceConstructor(descriptor.original)
            val irCall = IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset,
                context.irBuiltIns.unitType,
                constructorSymbol
            ).apply {
                context.callToSubstitutedDescriptorMap[this] = descriptor
                putTypeArguments(call.typeArguments) { it.toIrType() }
                this.dispatchReceiver = dispatchReceiver?.load()
                this.extensionReceiver = extensionReceiver?.load()
                contextReceiversCount = contextReceivers.size
            }
            addParametersToCall(startOffset, endOffset, call, irCall, context.irBuiltIns.unitType, contextReceivers.map { it.load() })
        }

    fun generateEnumConstructorSuperCall(startOffset: Int, endOffset: Int, call: CallBuilder): IrExpression {
        val constructorDescriptor = call.descriptor
        if (constructorDescriptor !is ClassConstructorDescriptor) throw AssertionError("Constructor expected: $constructorDescriptor")
        val classDescriptor = constructorDescriptor.containingDeclaration
        if (classDescriptor.kind != ClassKind.ENUM_CLASS) throw AssertionError("Enum class constructor expected: $classDescriptor")

        return call.callReceiver.call { dispatchReceiver, extensionReceiver, contextReceivers ->
            if (dispatchReceiver != null) throw AssertionError("Dispatch receiver should be null: $dispatchReceiver")
            if (extensionReceiver != null) throw AssertionError("Extension receiver should be null: $extensionReceiver")
            if (contextReceivers.isNotEmpty()) throw AssertionError("Context receivers should be empty: $contextReceivers")
            val descriptor = constructorDescriptor.original
            val constructorSymbol = context.symbolTable.descriptorExtension.referenceConstructor(descriptor)
            val returnType = constructorDescriptor.returnType.toIrType()
            val irCall = IrEnumConstructorCallImpl(
                startOffset, endOffset, returnType, constructorSymbol, descriptor.typeParametersCount, descriptor.valueParameters.size
            )
            context.callToSubstitutedDescriptorMap[irCall] = constructorDescriptor
            addParametersToCall(startOffset, endOffset, call, irCall, irCall.type, emptyList())
        }
    }

    private fun generatePropertyGetterCall(
        descriptor: PropertyDescriptor,
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder
    ): IrExpression {
        val getMethodDescriptor = descriptor.unwrappedGetMethod
        val irType = descriptor.type.toIrType()

        return if (getMethodDescriptor == null) {
            val superQualifierSymbol = (call.superQualifier ?: descriptor.containingDeclaration as? ClassDescriptor)?.let {
                if (it is ScriptDescriptor) null // otherwise it creates a reference to script as class; TODO: check if correct
                else context.symbolTable.descriptorExtension.referenceClass(it)
            }
            val fieldSymbol =
                context.symbolTable.descriptorExtension.referenceField(context.extensions.remapDebuggerFieldPropertyDescriptor(descriptor.resolveFakeOverride().original))
            call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue, _ ->
                IrGetFieldImpl(
                    startOffset, endOffset,
                    fieldSymbol,
                    irType,
                    dispatchReceiverValue?.load() ?: extensionReceiverValue?.load(),
                    IrStatementOrigin.GET_PROPERTY,
                    superQualifierSymbol
                ).also { context.callToSubstitutedDescriptorMap[it] = descriptor }
            }
        } else {
            val superQualifierSymbol = call.superQualifier?.let { context.symbolTable.descriptorExtension.referenceClass(it) }
            call.callReceiver.adjustForCallee(getMethodDescriptor)
                .call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
                    if (descriptor.isDynamic()) {
                        val dispatchReceiver = getDynamicExpressionReceiver(dispatchReceiverValue, extensionReceiverValue, descriptor)

                        IrDynamicMemberExpressionImpl(
                            startOffset, endOffset,
                            irType,
                            descriptor.name.asString(),
                            dispatchReceiver
                        )
                    } else {
                        val getterSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(getMethodDescriptor.original)
                        IrCallImpl(
                            startOffset, endOffset,
                            irType,
                            getterSymbol,
                            descriptor.typeParametersCount,
                            descriptor.contextReceiverParameters.size,
                            IrStatementOrigin.GET_PROPERTY,
                            superQualifierSymbol
                        ).apply {
                            context.callToSubstitutedDescriptorMap[this] = computeSubstitutedSyntheticAccessor(
                                descriptor, getMethodDescriptor, descriptor.getter!!
                            )

                            putTypeArguments(call.typeArguments) { it.toIrType() }
                            dispatchReceiver = dispatchReceiverValue?.load()
                            extensionReceiver = extensionReceiverValue?.load()
                            val contextReceivers = contextReceiverValues.map { it.load() }
                            contextReceiversCount = contextReceivers.size
                            addParametersToCall(startOffset, endOffset, call, this, irType, contextReceivers)
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

    private fun generateConstructorCall(
        constructorDescriptor: ClassConstructorDescriptor,
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        call: CallBuilder
    ): IrExpression =
        call.callReceiver.call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            val irType = constructorDescriptor.returnType.toIrType()
            val originalSymbol = context.symbolTable.descriptorExtension.referenceConstructor(constructorDescriptor.original)

            IrConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset,
                irType,
                originalSymbol,
                origin
            ).run {
                context.callToSubstitutedDescriptorMap[this] = constructorDescriptor
                putTypeArguments(call.typeArguments) { it.toIrType() }
                dispatchReceiver = dispatchReceiverValue?.load()
                extensionReceiver = extensionReceiverValue?.load()
                val contextReceivers = contextReceiverValues.map { it.load() }
                addParametersToCall(startOffset, endOffset, call, this, irType, contextReceivers)
            }
        }

    internal fun generateFunctionCall(
        functionDescriptor: FunctionDescriptor,
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        call: CallBuilder
    ): IrExpression {
        val builder = CallExpressionBuilder { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            if (functionDescriptor.isDynamic()) {
                generateDynamicFunctionCall(startOffset, endOffset, functionDescriptor, call, dispatchReceiverValue, extensionReceiverValue)
            } else {
                val originalSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(functionDescriptor.original)
                IrCallImpl.fromSymbolDescriptor(
                    startOffset, endOffset,
                    functionDescriptor.returnType!!.toIrType(),
                    originalSymbol,
                    origin = origin,
                    superQualifierSymbol = call.superQualifier?.let { context.symbolTable.descriptorExtension.referenceClass(it) }
                ).run {
                    context.callToSubstitutedDescriptorMap[this] = functionDescriptor
                    putTypeArguments(call.typeArguments) { it.toIrType() }
                    dispatchReceiver = dispatchReceiverValue?.load()
                    extensionReceiver = extensionReceiverValue?.load()
                    val contextReceivers = contextReceiverValues.map { it.load() }
                    contextReceiversCount = contextReceivers.size
                    addParametersToCall(startOffset, endOffset, call, this, type, contextReceivers)
                }
            }
        }
        return with(call.callReceiver) {
            when (this) {
                is SimpleCallReceiver -> builder.withReceivers(dispatchReceiverValue, extensionReceiverValue, contextReceiverValues)
                else -> call(builder)
            }
        }
    }

    private fun generateDynamicFunctionCall(
        startOffset: Int,
        endOffset: Int,
        functionDescriptor: FunctionDescriptor,
        call: CallBuilder,
        dispatchReceiverValue: IntermediateValue?,
        extensionReceiverValue: IntermediateValue?,
    ): IrDynamicOperatorExpressionImpl {
        fun makeDynamicOperatorExpression(operator: IrDynamicOperator) =
            IrDynamicOperatorExpressionImpl(
                startOffset, endOffset,
                functionDescriptor.returnType!!.toIrType(),
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

        return when {
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
    }

    private fun addParametersToCall(
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder,
        irCall: IrFunctionAccessExpression,
        irResultType: IrType,
        contextReceivers: List<IrExpression>
    ): IrExpression {
        contextReceivers.forEachIndexed(irCall::putValueArgument)
        return if (call.isValueArgumentReorderingRequired()) {
            generateCallWithArgumentReordering(irCall, startOffset, endOffset, call, irResultType, contextReceivers.size)
        } else {
            val valueArguments = call.getValueArgumentsInParameterOrder()
            for ((index, valueArgument) in valueArguments.withIndex()) {
                irCall.putValueArgument(index + contextReceivers.size, valueArgument)
            }
            irCall
        }
    }

    private fun generateCallWithArgumentReordering(
        irCall: IrFunctionAccessExpression,
        startOffset: Int,
        endOffset: Int,
        call: CallBuilder,
        irResultType: IrType,
        contextReceiversCount: Int
    ): IrExpression {
        val irBlock = IrBlockImpl(startOffset, endOffset, irResultType, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL)

        fun IrExpression.freeze(nameHint: String) =
            if (isUnchanging())
                this
            else
                scope.createTemporaryVariableInBlock(context, this, irBlock, nameHint).load()

        irCall.dispatchReceiver = irCall.dispatchReceiver?.freeze("\$this")
        irCall.extensionReceiver = irCall.extensionReceiver?.freeze("\$receiver")

        val resolvedCall = call.original
        val valueParameters = resolvedCall.resultingDescriptor.valueParameters
        val valueArgumentsToIndex = HashMap<ResolvedValueArgument, Int>()
        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            valueArgumentsToIndex[valueArgument] = index
        }
        for (valueArgument in resolvedCall.valueArguments.values) {
            val index = valueArgumentsToIndex[valueArgument]!!
            val irArgument = call.getValueArgument(valueParameters[index]) ?: continue
            irCall.putValueArgument(index + contextReceiversCount, irArgument.freeze(valueParameters[index].name.asString()))
        }
        irBlock.statements.add(irCall)
        return irBlock
    }
}

internal fun CallGenerator.generateCall(
    ktElement: KtElement,
    call: CallBuilder,
    origin: IrStatementOrigin? = null,
    startOffset: Int = ktElement.startOffsetSkippingComments,
    endOffset: Int = ktElement.endOffset,
): IrExpression =
    generateCall(startOffset, endOffset, call, origin)

internal fun CallGenerator.generateCall(irExpression: IrExpression, call: CallBuilder, origin: IrStatementOrigin? = null): IrExpression =
    generateCall(irExpression.startOffset, irExpression.endOffset, call, origin)
