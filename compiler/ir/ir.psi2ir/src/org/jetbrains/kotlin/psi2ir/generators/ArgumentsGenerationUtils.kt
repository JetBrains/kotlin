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

import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.util.getSuperCallExpression
import org.jetbrains.kotlin.resolve.calls.util.isSafeCall
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.max
import kotlin.math.min

internal fun StatementGenerator.generateReceiverOrNull(ktDefaultElement: KtElement, receiver: ReceiverValue?): IntermediateValue? =
    receiver?.let { generateReceiver(ktDefaultElement, receiver) }

private fun StatementGenerator.generateContextReceiverForDelegatingConstructorCall(
    ktDefaultElement: KtElement,
    receiver: ContextClassReceiver?
): IntermediateValue? =
    receiver?.let {
        generateContextReceiverForDelegatingConstructorCall(
            ktDefaultElement.startOffsetSkippingComments,
            ktDefaultElement.endOffset,
            receiver
        )
    }

private fun StatementGenerator.generateContextReceiverForDelegatingConstructorCall(
    defaultStartOffset: Int,
    defaultEndOffset: Int,
    receiver: ContextClassReceiver
): IntermediateValue {
    val irReceiverType = receiver.type.toIrType()
    val contextReceivers = receiver.classDescriptor.contextReceivers
    val receiverParameter = contextReceivers.single { it.value == receiver }
    return object : ExpressionValue(irReceiverType) {
        override fun load(): IrExpression = IrGetValueImpl(
            defaultStartOffset, defaultEndOffset, irReceiverType,
            context.symbolTable.descriptorExtension.referenceValueParameter(receiverParameter)
        )
    }
}

private fun StatementGenerator.generateReceiver(ktDefaultElement: KtElement, receiver: ReceiverValue): IntermediateValue =
    generateReceiver(ktDefaultElement.startOffsetSkippingComments, ktDefaultElement.endOffset, receiver)

private fun StatementGenerator.generateReceiver(defaultStartOffset: Int, defaultEndOffset: Int, receiver: ReceiverValue): IntermediateValue {
    val irReceiverType =
        when (receiver) {
            is ExtensionReceiver ->
                receiver.declarationDescriptor.extensionReceiverParameter!!.type.toIrType()
            is ContextReceiver -> {
                val receiverParameter = receiver.declarationDescriptor.contextReceiverParameters.find {
                    it.value == receiver.original
                } ?: error("Unknown receiver: $receiver")
                receiverParameter.type.toIrType()
            }
            else ->
                receiver.type.toIrType()
        }

    if (receiver is TransientReceiver) return TransientReceiverValue(irReceiverType)

    return object : ExpressionValue(irReceiverType) {
        override fun load(): IrExpression =
            when (receiver) {
                is ImplicitClassReceiver -> {
                    val receiverClassDescriptor = receiver.classDescriptor
                    if (shouldGenerateReceiverAsSingletonReference(receiverClassDescriptor))
                        generateSingletonReference(receiverClassDescriptor, defaultStartOffset, defaultEndOffset, receiver.type)
                    else
                        IrGetValueImpl(
                            defaultStartOffset, defaultEndOffset, irReceiverType,
                            context.symbolTable.descriptorExtension.referenceValueParameter(receiverClassDescriptor.thisAsReceiverParameter)
                        )
                }
                is ContextClassReceiver -> loadContextReceiver(receiver, defaultStartOffset, defaultEndOffset)
                is ThisClassReceiver ->
                    generateThisOrSuperReceiver(receiver, receiver.classDescriptor)
                is SuperCallReceiverValue ->
                    generateThisOrSuperReceiver(receiver, receiver.thisType.constructor.declarationDescriptor as ClassDescriptor)
                is ExpressionReceiver ->
                    generateStatement(receiver.expression) as IrExpression
                is ExtensionReceiver -> {
                    IrGetValueImpl(
                        defaultStartOffset, defaultStartOffset, irReceiverType,
                        context.symbolTable.descriptorExtension.referenceValueParameter(receiver.declarationDescriptor.extensionReceiverParameter!!)
                    )
                }
                is ContextReceiver -> {
                    val receiverParameter = receiver.declarationDescriptor.contextReceiverParameters
                        .single { it.value == receiver.original }
                    IrGetValueImpl(
                        defaultStartOffset, defaultStartOffset, irReceiverType,
                        context.symbolTable.descriptorExtension.referenceValueParameter(receiverParameter)
                    )
                }
                else ->
                    throw AssertionError("Unexpected receiver: ${receiver::class.java.simpleName}")
            }
    }
}

internal fun StatementGenerator.loadContextReceiver(
    receiver: ContextClassReceiver,
    defaultStartOffset: Int, defaultEndOffset: Int,
): IrGetFieldImpl {
    val receiverClassDescriptor = receiver.classDescriptor
    val thisAsReceiverParameter = receiverClassDescriptor.thisAsReceiverParameter
    val thisReceiver = IrGetValueImpl(
        defaultStartOffset, defaultEndOffset,
        thisAsReceiverParameter.type.toIrType(),
        context.symbolTable.descriptorExtension.referenceValue(thisAsReceiverParameter)
    )

    return IrGetFieldImpl(
        defaultStartOffset, defaultEndOffset,
        context.additionalDescriptorStorage.getSyntheticField(receiver).symbol,
        receiver.type.toIrType(), thisReceiver
    )
}


internal fun StatementGenerator.generateSingletonReference(
    descriptor: ClassDescriptor,
    startOffset: Int,
    endOffset: Int,
    type: KotlinType
): IrDeclarationReference {
    val irType = type.toIrType()

    return when {
        DescriptorUtils.isObject(descriptor) ->
            IrGetObjectValueImpl(
                startOffset, endOffset, irType,
                context.symbolTable.descriptorExtension.referenceClass(descriptor)
            )
        DescriptorUtils.isEnumEntry(descriptor) ->
            IrGetEnumValueImpl(
                startOffset, endOffset, irType,
                context.symbolTable.descriptorExtension.referenceEnumEntry(descriptor)
            )
        else -> {
            val companionObjectDescriptor = descriptor.companionObjectDescriptor
                ?: throw java.lang.AssertionError("Class value without companion object: $descriptor")
            IrGetObjectValueImpl(
                startOffset, endOffset, irType,
                context.symbolTable.descriptorExtension.referenceClass(companionObjectDescriptor)
            )
        }
    }
}

private fun StatementGenerator.shouldGenerateReceiverAsSingletonReference(receiverClassDescriptor: ClassDescriptor): Boolean {
    val scopeOwner = this.scopeOwner
    return receiverClassDescriptor.kind.isSingleton &&
            scopeOwner != receiverClassDescriptor && // For anonymous initializers
            !(scopeOwner is CallableMemberDescriptor && scopeOwner.containingDeclaration == receiverClassDescriptor) // Members of object
}

private fun StatementGenerator.generateThisOrSuperReceiver(receiver: ReceiverValue, classDescriptor: ClassDescriptor): IrExpression {
    val expressionReceiver = receiver as? ExpressionReceiver
        ?: throw AssertionError("'this' or 'super' receiver should be an expression receiver")
    val ktReceiver = expressionReceiver.expression
    val type = if (receiver is SuperCallReceiverValue) receiver.thisType else expressionReceiver.type
    return generateThisReceiver(ktReceiver.startOffsetSkippingComments, ktReceiver.endOffset, type, classDescriptor)
}

internal fun StatementGenerator.generateBackingFieldReceiver(
    startOffset: Int,
    endOffset: Int,
    resolvedCall: ResolvedCall<*>?,
    fieldDescriptor: SyntheticFieldDescriptor
): IntermediateValue? {
    val receiver = resolvedCall?.dispatchReceiver ?: fieldDescriptor.getDispatchReceiverForBackend() ?: return null
    return this.generateReceiver(startOffset, endOffset, receiver)
}

internal fun StatementGenerator.generateCallReceiver(
    ktDefaultElement: KtElement,
    calleeDescriptor: CallableDescriptor,
    dispatchReceiver: ReceiverValue?,
    extensionReceiver: ReceiverValue?,
    contextReceivers: List<ReceiverValue>,
    isSafe: Boolean,
    isAssignmentReceiver: Boolean = false
): CallReceiver {
    val dispatchReceiverValue: IntermediateValue?
    val extensionReceiverValue: IntermediateValue?
    val contextReceiverValues: List<IntermediateValue>
    val startOffset = ktDefaultElement.startOffsetSkippingComments
    val endOffset = ktDefaultElement.endOffset
    when (calleeDescriptor) {
        is ImportedFromObjectCallableDescriptor<*> -> {
            assert(dispatchReceiver == null) {
                "Call for member imported from object $calleeDescriptor has non-null dispatch receiver $dispatchReceiver"
            }
            dispatchReceiverValue = generateReceiverForCalleeImportedFromObject(startOffset, endOffset, calleeDescriptor)
            extensionReceiverValue = generateReceiverOrNull(ktDefaultElement, extensionReceiver)
            contextReceiverValues = contextReceivers.mapNotNull { generateReceiverOrNull(ktDefaultElement, it) }
        }
        is TypeAliasConstructorDescriptor -> {
            assert(!(dispatchReceiver != null && extensionReceiver != null)) {
                "Type alias constructor call for $calleeDescriptor can't have both dispatch receiver and extension receiver: " +
                        "$dispatchReceiver, $extensionReceiver"
            }
            dispatchReceiverValue = generateReceiverOrNull(ktDefaultElement, extensionReceiver ?: dispatchReceiver)
            extensionReceiverValue = null
            contextReceiverValues = contextReceivers.mapNotNull { generateReceiverOrNull(ktDefaultElement, it) }
        }
        else -> {
            dispatchReceiverValue = generateReceiverOrNull(ktDefaultElement, dispatchReceiver)
            extensionReceiverValue = generateReceiverOrNull(ktDefaultElement, extensionReceiver)
            contextReceiverValues = when (ktDefaultElement) {
                is KtConstructorDelegationCall, is KtSuperTypeCallEntry -> contextReceivers.mapNotNull {
                    if (it is ContextClassReceiver) generateContextReceiverForDelegatingConstructorCall(ktDefaultElement, it)
                    else generateReceiverOrNull(ktDefaultElement, it)
                }
                else -> contextReceivers.mapNotNull { generateReceiverOrNull(ktDefaultElement, it) }
            }
        }
    }

    return when {
        !isSafe ->
            SimpleCallReceiver(dispatchReceiverValue, extensionReceiverValue, contextReceiverValues)
        extensionReceiverValue != null || dispatchReceiverValue != null ->
            SafeCallReceiver(
                this, startOffset, endOffset,
                extensionReceiverValue, contextReceiverValues, dispatchReceiverValue, isAssignmentReceiver
            )
        else ->
            throw AssertionError("Safe call should have an explicit receiver: ${ktDefaultElement.text}")
    }
}

private fun StatementGenerator.generateReceiverForCalleeImportedFromObject(
    startOffset: Int,
    endOffset: Int,
    calleeDescriptor: ImportedFromObjectCallableDescriptor<*>
): ExpressionValue {
    val objectDescriptor = calleeDescriptor.containingObject
    val objectType = objectDescriptor.defaultType.toIrType()
    return generateExpressionValue(objectType) {
        IrGetObjectValueImpl(
            startOffset, endOffset, objectType,
            context.symbolTable.descriptorExtension.referenceClass(objectDescriptor)
        )
    }
}

private fun StatementGenerator.computeVarargType(type: KotlinType): IrType =
    // Vararg type loaded from Java can be flexible, and have `Nothing` as lower bound after approximation. (See KT-52146.)
    // Its upper bound should always have the form `Array<out T>`, though.
    type.upperIfFlexible().toIrType()

private fun StatementGenerator.generateVarargExpressionUsing(
    varargArgument: VarargValueArgument,
    valueParameter: ValueParameterDescriptor,
    resolvedCall: ResolvedCall<*>,
    generateArgumentExpression: (KtExpression) -> IrExpression?
): IrExpression? {
    if (varargArgument.arguments.isEmpty()) {
        return null
    }

    val varargStartOffset = varargArgument.arguments.fold(Int.MAX_VALUE) { minStartOffset, argument ->
        min(minStartOffset, argument.asElement().startOffsetSkippingComments)
    }
    val varargEndOffset = varargArgument.arguments.fold(Int.MIN_VALUE) { maxEndOffset, argument ->
        max(maxEndOffset, argument.asElement().endOffset)
    }

    val varargElementType =
        valueParameter.varargElementType ?: throw AssertionError("Vararg argument for non-vararg parameter $valueParameter")

    val irVararg = IrVarargImpl(varargStartOffset, varargEndOffset, computeVarargType(valueParameter.type), varargElementType.toIrType())

    for (varargElementArgument in varargArgument.arguments) {
        val ktArgumentExpression = varargElementArgument.getArgumentExpression()
            ?: throw AssertionError("No argument expression for vararg element ${varargElementArgument.asElement().text}")
        val irArgumentExpression =
            generateArgumentExpression(ktArgumentExpression)
                ?.let { irArg ->
                    applySuspendConversionForValueArgumentIfRequired(irArg, varargElementArgument, valueParameter, resolvedCall)
                }
                ?: throw AssertionError("no expression for vararg element ${ktArgumentExpression.text}")

        val irVarargElement =
            if (varargElementArgument.getSpreadElement() != null ||
                context.languageVersionSettings
                    .supportsFeature(LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions) &&
                varargElementArgument.isNamed()
            )
                IrSpreadElementImpl(
                    ktArgumentExpression.startOffsetSkippingComments, ktArgumentExpression.endOffset,
                    irArgumentExpression
                )
            else
                irArgumentExpression

        irVararg.addElement(irVarargElement)
    }

    return irVararg
}

private fun StatementGenerator.generateValueArgument(
    valueArgument: ResolvedValueArgument,
    valueParameter: ValueParameterDescriptor,
    resolvedCall: ResolvedCall<*>
): IrExpression? = generateValueArgumentUsing(valueArgument, valueParameter, resolvedCall) { generateExpression(it) }

private fun StatementGenerator.generateValueArgumentUsing(
    valueArgument: ResolvedValueArgument,
    valueParameter: ValueParameterDescriptor,
    resolvedCall: ResolvedCall<*>,
    generateArgumentExpression: (KtExpression) -> IrExpression?
): IrExpression? =
    when (valueArgument) {
        is DefaultValueArgument ->
            null
        is ExpressionValueArgument -> {
            val valueArgument1 = valueArgument.valueArgument
                ?: throw AssertionError("No value argument: $valueArgument")
            val argumentExpression = valueArgument1.getArgumentExpression()
                ?: throw AssertionError("No argument expression: $valueArgument1")
            generateArgumentExpression(argumentExpression)?.let { expression ->
                applySuspendConversionForValueArgumentIfRequired(expression, valueArgument1, valueParameter, resolvedCall)
            }
        }
        is VarargValueArgument ->
            generateVarargExpressionUsing(valueArgument, valueParameter, resolvedCall, generateArgumentExpression)
        else ->
            TODO("Unexpected valueArgument: ${valueArgument::class.java.simpleName}")
    }

private fun StatementGenerator.applySuspendConversionForValueArgumentIfRequired(
    expression: IrExpression,
    valueArgument: ValueArgument,
    valueParameter: ValueParameterDescriptor,
    resolvedCall: ResolvedCall<*>
): IrExpression {
    if (!context.languageVersionSettings.supportsFeature(LanguageFeature.SuspendConversion))
        return expression

    if (expression is IrBlock && expression.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)
        return expression

    val newResolvedCall = resolvedCall as? NewResolvedCallImpl<*>
        ?: return expression

    val suspendConversionType = newResolvedCall.getExpectedTypeForSuspendConvertedArgument(valueArgument)
        ?: return expression

    val valueParameterType = if (valueParameter.isVararg) valueParameter.varargElementType!! else valueParameter.type

    val suspendFunType: KotlinType =
        if (context.extensions.samConversion.isSamType(valueParameterType))
            valueParameterType.getSubstitutedFunctionTypeForSamType()
        else
            valueParameterType

    val irAdapterRefType = suspendFunType.toIrType()
    return IrBlockImpl(expression.startOffset, expression.endOffset, irAdapterRefType, IrStatementOrigin.SUSPEND_CONVERSION)
        .apply {
            val irAdapterFunction = createFunctionForSuspendConversion(startOffset, endOffset, suspendConversionType, suspendFunType)
            // TODO add a bound receiver property to IrFunctionExpressionImpl?
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, irAdapterRefType, irAdapterFunction.symbol, irAdapterFunction.typeParameters.size,
                irAdapterFunction.valueParameters.size, null, IrStatementOrigin.SUSPEND_CONVERSION
            )
            statements.add(irAdapterFunction)
            statements.add(irAdapterRef.apply { extensionReceiver = expression })
        }
}

private fun StatementGenerator.createFunctionForSuspendConversion(
    startOffset: Int,
    endOffset: Int,
    funType: KotlinType,
    suspendFunType: KotlinType
): IrSimpleFunction {
    val irFunReturnType = funType.arguments.last().type.toIrType()
    val irSuspendFunReturnType = suspendFunType.arguments.last().type.toIrType()

    val irAdapterFun = context.irFactory.createSimpleFunction(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION,
        name = Name.identifier(scope.inventNameForTemporary("suspendConversion")),
        visibility = DescriptorVisibilities.LOCAL,
        isInline = false,
        isExpect = false,
        returnType = irSuspendFunReturnType,
        modality = Modality.FINAL,
        symbol = IrSimpleFunctionSymbolImpl(),
        isTailrec = false,
        isSuspend = true,
        isOperator = false,
        isInfix = false,
        isExternal = false,
    )

    context.symbolTable.enterScope(irAdapterFun)

    fun createValueParameter(name: String, index: Int, type: IrType): IrValueParameter =
        context.irFactory.createValueParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION,
            name = Name.identifier(name),
            type = type,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            index = index,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )

    irAdapterFun.extensionReceiverParameter = createValueParameter("callee", -1, funType.toIrType())
    irAdapterFun.valueParameters = suspendFunType.arguments
        .take(suspendFunType.arguments.size - 1)
        .mapIndexed { index, typeProjection -> createValueParameter("p$index", index, typeProjection.type.toIrType()) }

    val valueArgumentsCount = irAdapterFun.valueParameters.size
    val invokeDescriptor = funType.memberScope
        .getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND)
        .find { it.valueParameters.size == valueArgumentsCount }
        ?: error("No matching operator fun 'invoke' for suspend conversion: funType=$funType, suspendFunType=$suspendFunType")
    val invokeSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(invokeDescriptor.original)

    irAdapterFun.body = irBlockBody(startOffset, endOffset) {
        val irAdapteeCall = IrCallImpl(
            startOffset, endOffset, irFunReturnType,
            invokeSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = valueArgumentsCount
        )

        irAdapteeCall.dispatchReceiver = irGet(irAdapterFun.extensionReceiverParameter!!)

        this@createFunctionForSuspendConversion.context
            .callToSubstitutedDescriptorMap[irAdapteeCall] = invokeDescriptor

        for (irAdapterParameter in irAdapterFun.valueParameters) {
            irAdapteeCall.putValueArgument(irAdapterParameter.index, irGet(irAdapterParameter))
        }
        if (suspendFunType.arguments.last().type.isUnit()) {
            +irAdapteeCall
        } else {
            +IrReturnImpl(
                startOffset, endOffset,
                context.irBuiltIns.nothingType,
                irAdapterFun.symbol,
                irAdapteeCall
            )
        }
    }

    context.symbolTable.leaveScope(irAdapterFun)

    return irAdapterFun
}

internal fun StatementGenerator.castArgumentToFunctionalInterfaceForSamType(irExpression: IrExpression, samType: KotlinType): IrExpression {
    val kotlinFunctionType = samType.getSubstitutedFunctionTypeForSamType()
    val irFunctionType = context.typeTranslator.translateType(kotlinFunctionType)
    return irExpression.implicitCastTo(irFunctionType)
}

internal fun Generator.getSuperQualifier(resolvedCall: ResolvedCall<*>): ClassDescriptor? {
    val superCallExpression = getSuperCallExpression(resolvedCall.call) ?: return null
    return getOrFail(BindingContext.REFERENCE_TARGET, superCallExpression.instanceReference) as ClassDescriptor
}

internal fun StatementGenerator.pregenerateCall(resolvedCall: ResolvedCall<*>): CallBuilder =
    pregenerateCallUsing(resolvedCall) { generateExpression(it) }

internal fun StatementGenerator.pregenerateCallUsing(
    resolvedCall: ResolvedCall<*>,
    generateArgumentExpression: (KtExpression) -> IrExpression?
): CallBuilder {
    if (resolvedCall.isExtensionInvokeCall()) {
        return pregenerateExtensionInvokeCall(resolvedCall)
    }
    val call = pregenerateCallReceivers(resolvedCall)
    pregenerateValueArgumentsUsing(call, resolvedCall, generateArgumentExpression)
    generateSamConversionForValueArgumentsIfRequired(call, resolvedCall)
    return call
}

internal fun getTypeArguments(resolvedCall: ResolvedCall<*>?): Map<TypeParameterDescriptor, KotlinType>? {
    if (resolvedCall == null) return null

    val descriptor = resolvedCall.resultingDescriptor
    if (descriptor.typeParameters.isEmpty()) return null

    return resolvedCall.typeArguments
}


private fun StatementGenerator.pregenerateExtensionInvokeCall(resolvedCall: ResolvedCall<*>): CallBuilder {
    val extensionInvoke = resolvedCall.resultingDescriptor
    val functionNClass = extensionInvoke.containingDeclaration as? ClassDescriptor
        ?: throw AssertionError("'invoke' should be a class member: $extensionInvoke")
    val unsubstitutedPlainInvokes =
        functionNClass.unsubstitutedMemberScope.getContributedFunctions(extensionInvoke.name, NoLookupLocation.FROM_BACKEND)
    val unsubstitutedPlainInvoke = unsubstitutedPlainInvokes.singleOrNull()
        ?: throw AssertionError("There should be a single 'invoke' in FunctionN class: $unsubstitutedPlainInvokes")

    assert(unsubstitutedPlainInvoke.typeParameters.isEmpty()) {
        "'operator fun invoke' should have no type parameters: $unsubstitutedPlainInvoke"
    }

    val expectedValueParametersCount = extensionInvoke.valueParameters.size + 1
    assert(unsubstitutedPlainInvoke.valueParameters.size == expectedValueParametersCount) {
        "Plain 'invoke' should have $expectedValueParametersCount value parameters, got ${unsubstitutedPlainInvoke.valueParameters}"
    }

    val functionNType = extensionInvoke.dispatchReceiverParameter!!.type
    val plainInvoke = unsubstitutedPlainInvoke.substitute(TypeSubstitutor.create(functionNType))
        ?: throw AssertionError("Substitution failed for $unsubstitutedPlainInvoke, type=$functionNType")

    val ktCallElement = resolvedCall.call.callElement

    val call = CallBuilder(
        resolvedCall,
        plainInvoke,
        typeArguments = null, // FunctionN#invoke has no type parameters of its own
        isExtensionInvokeCall = true
    )

    val functionReceiverValue = run {
        val dispatchReceiver =
            resolvedCall.dispatchReceiver ?: throw AssertionError("Extension 'invoke' call should have a dispatch receiver")
        generateReceiver(ktCallElement, dispatchReceiver)
    }

    val extensionInvokeReceiverValue = run {
        val extensionReceiver =
            resolvedCall.extensionReceiver ?: throw AssertionError("Extension 'invoke' call should have an extension receiver")
        generateReceiver(ktCallElement, extensionReceiver)
    }

    call.callReceiver =
        if (resolvedCall.call.isSafeCall())
            SafeExtensionInvokeCallReceiver(
                this, ktCallElement.startOffsetSkippingComments, ktCallElement.endOffset,
                call, functionReceiverValue, extensionInvokeReceiverValue
            )
        else
            ExtensionInvokeCallReceiver(call, functionReceiverValue, extensionInvokeReceiverValue)

    call.irValueArgumentsByIndex[0] = null
    for ((valueParameter, valueArgument) in resolvedCall.valueArguments) {
        call.irValueArgumentsByIndex[valueParameter.index + 1] =
            generateValueArgument(valueArgument, valueParameter, resolvedCall)
    }

    return call
}

private fun ResolvedCall<*>.isExtensionInvokeCall(): Boolean {
    val callee = resultingDescriptor as? SimpleFunctionDescriptor ?: return false
    if (callee.name.asString() != "invoke") return false
    val dispatchReceiverType = callee.dispatchReceiverParameter?.type ?: return false
    if (!dispatchReceiverType.isBuiltinFunctionalType) return false
    return extensionReceiver != null
}

internal fun StatementGenerator.generateSamConversionForValueArgumentsIfRequired(call: CallBuilder, resolvedCall: ResolvedCall<*>) {
    val samConversion = context.extensions.samConversion

    val originalDescriptor = resolvedCall.resultingDescriptor
    val underlyingDescriptor = originalDescriptor.getOriginalForFunctionInterfaceAdapter() ?: originalDescriptor

    val originalValueParameters = originalDescriptor.valueParameters
    val underlyingValueParameters = underlyingDescriptor.valueParameters

    assert(originalValueParameters.size == underlyingValueParameters.size) {
        "Mismatching value parameters, $originalDescriptor vs $underlyingDescriptor: " +
                "${originalValueParameters.size} != ${underlyingValueParameters.size}"
    }
    assert(originalValueParameters.size == call.argumentsCount) {
        "Mismatching value parameters, $originalDescriptor vs call: " +
                "${originalValueParameters.size} != ${call.argumentsCount}"
    }
    assert(underlyingDescriptor.typeParameters.size == originalDescriptor.typeParameters.size) {
        "Mismatching type parameters:\n" +
                "$underlyingDescriptor has ${underlyingDescriptor.typeParameters}\n" +
                "$originalDescriptor has ${originalDescriptor.typeParameters}"
    }

    val resolvedCallArguments = (resolvedCall as? NewResolvedCallImpl<*>)?.argumentMappingByOriginal?.values
    assert(resolvedCallArguments == null || resolvedCallArguments.size == underlyingValueParameters.size) {
        "Mismatching resolved call arguments:\n" +
                "${resolvedCallArguments?.size} != ${underlyingValueParameters.size}"
    }

    val substitutionContext = call.original.typeArguments.entries.associate { (typeParameterDescriptor, typeArgument) ->
        underlyingDescriptor.typeParameters[typeParameterDescriptor.index].typeConstructor to TypeProjectionImpl(typeArgument)
    }
    val typeSubstitutor = TypeSubstitutor.create(substitutionContext)

    for (i in underlyingValueParameters.indices) {
        val underlyingValueParameter: ValueParameterDescriptor = underlyingValueParameters[i]

        val expectedSamConversionTypesForVararg =
            if (resolvedCall is NewResolvedCallImpl<*>) {
                val arguments = resolvedCall.valueArguments[originalValueParameters[i]]?.arguments
                arguments?.map { resolvedCall.getExpectedTypeForSamConvertedArgument(it) }
            } else null

        if (expectedSamConversionTypesForVararg == null || expectedSamConversionTypesForVararg.all { it == null }) {
            // When the method is `f(T)` with `T` = a SAM type, the substituted type is a SAM while the original is not;
            // when the method is `f(X<T>)` with `T` = `out V` where `X` is a SAM type, the substituted type is `Nothing`
            // while the original is a SAM interface. Thus, if *either* of those is a SAM type then it's fine.
            if (!samConversion.isSamType(underlyingValueParameter.type) &&
                !samConversion.isSamType(underlyingValueParameter.original.type)
            ) continue
            if (!originalValueParameters[i].type.isFunctionType) continue
        }

        val samKotlinType = getSamTypeForValueParameter(underlyingValueParameter)
            ?: underlyingValueParameter.varargElementType // If we have a vararg, vararg element type will be taken
            ?: underlyingValueParameter.type

        val originalArgument = call.irValueArgumentsByIndex[i] ?: continue

        val substitutedSamType = typeSubstitutor.substitute(samKotlinType, Variance.INVARIANT)
            ?: throw AssertionError(
                "Failed to substitute value argument type in SAM conversion: " +
                        "underlyingParameterType=${underlyingValueParameter.type}, " +
                        "substitutionContext=$substitutionContext"
            )

        val irSamType = substitutedSamType.toIrType()

        fun IrExpression.isFunctionReferenceAdapter() =
            this is IrBlock && (origin == IrStatementOrigin.SUSPEND_CONVERSION || origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)

        fun IrExpression.applySamConversion() =
            IrTypeOperatorCallImpl(
                startOffset, endOffset,
                irSamType,
                IrTypeOperator.SAM_CONVERSION, irSamType,
                castArgumentToFunctionalInterfaceForSamType(this, substitutedSamType)
            )

        fun samConvertScalarExpression(irArgument: IrExpression) =
            if (irArgument.isFunctionReferenceAdapter()) {
                // Apply SAM_CONVERSION directly to the adapter reference
                val irBlock = irArgument as IrBlock
                irBlock.type = irSamType
                val irAdapterRef = irBlock.statements.last() as IrFunctionReference
                irBlock.statements[irBlock.statements.lastIndex] = irAdapterRef.applySamConversion()
                irBlock
            } else {
                irArgument.applySamConversion()
            }

        call.irValueArgumentsByIndex[i] =
            if (originalArgument !is IrVararg) {
                samConvertScalarExpression(originalArgument)
            } else {
                if (underlyingValueParameter.varargElementType == null) {
                    throw AssertionError("Vararg parameter expected for vararg argument: $underlyingValueParameter")
                }

                val substitutedVarargType =
                    typeSubstitutor.substitute(underlyingValueParameter.type, Variance.INVARIANT)
                        ?: throw AssertionError(
                            "Failed to substitute vararg type in SAM conversion: " +
                                    "type=${underlyingValueParameter.type}, " +
                                    "substitutionContext=$substitutionContext"
                        )

                IrVarargImpl(
                    originalArgument.startOffset, originalArgument.endOffset,
                    computeVarargType(substitutedVarargType),
                    irSamType
                ).apply {
                    originalArgument.elements.mapIndexedTo(elements) { index, element ->
                        if (element is IrExpression) {
                            if (expectedSamConversionTypesForVararg?.get(index) != null)
                                samConvertScalarExpression(element)
                            else
                                element
                        } else {
                            throw AssertionError("Unsupported: spread vararg element with SAM conversion")
                        }
                    }
                }
            }
    }
}

private fun StatementGenerator.getSamTypeForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? {
    val approximatedSamType = context.samTypeApproximator.getSamTypeForValueParameter(
        valueParameter, context.extensions.samConversion.isCarefulApproximationOfContravariantProjection(),
    ) ?: return null
    if (!context.extensions.samConversion.isSamType(approximatedSamType))
        return null
    val classDescriptor = approximatedSamType.constructor.declarationDescriptor
        ?: throw AssertionError("SAM type is expected to be a class type: $approximatedSamType")
    return approximatedSamType.replace(
        approximatedSamType.arguments.mapIndexed { index: Int, typeProjection: TypeProjection ->
            if (typeProjection.type.constructor.isDenotable)
                typeProjection
            else
                StarProjectionImpl(classDescriptor.typeConstructor.parameters[index])
        }
    )
}

internal fun StatementGenerator.pregenerateValueArgumentsUsing(
    call: CallBuilder,
    resolvedCall: ResolvedCall<*>,
    generateArgumentExpression: (KtExpression) -> IrExpression?
) {
    resolvedCall.valueArgumentsByIndex!!.forEachIndexed { index, valueArgument ->
        val valueParameter = call.descriptor.valueParameters[index]
        call.irValueArgumentsByIndex[index] =
            generateValueArgumentUsing(valueArgument, valueParameter, resolvedCall, generateArgumentExpression)
    }
}

internal fun StatementGenerator.pregenerateCallReceivers(resolvedCall: ResolvedCall<*>): CallBuilder {
    val call = unwrapCallableDescriptorAndTypeArguments(resolvedCall)

    call.callReceiver = generateCallReceiver(
        resolvedCall.call.callElement,
        resolvedCall.resultingDescriptor,
        resolvedCall.dispatchReceiver,
        resolvedCall.extensionReceiver,
        resolvedCall.contextReceivers,
        isSafe = resolvedCall.call.isSafeCall()
    )

    call.superQualifier = getSuperQualifier(resolvedCall)

    return call
}

private fun unwrapSpecialDescriptor(descriptor: CallableDescriptor): CallableDescriptor =
    when (descriptor) {
        is ImportedFromObjectCallableDescriptor<*> ->
            unwrapSpecialDescriptor(descriptor.callableFromObject)
        is TypeAliasConstructorDescriptor ->
            descriptor.underlyingConstructorDescriptor
        else ->
            descriptor.getOriginalForFunctionInterfaceAdapter()?.let { unwrapSpecialDescriptor(it) } ?: descriptor
    }

internal fun unwrapCallableDescriptorAndTypeArguments(resolvedCall: ResolvedCall<*>): CallBuilder {
    val originalDescriptor = resolvedCall.resultingDescriptor
    val candidateDescriptor = resolvedCall.candidateDescriptor

    val unwrappedDescriptor = unwrapSpecialDescriptor(originalDescriptor)

    val originalTypeArguments = resolvedCall.typeArguments
    val unsubstitutedUnwrappedDescriptor = unwrappedDescriptor.original
    val unsubstitutedUnwrappedTypeParameters = unsubstitutedUnwrappedDescriptor.typeParameters

    val unwrappedTypeArguments = when (originalDescriptor) {
        is ImportedFromObjectCallableDescriptor<*> -> {
            assert(originalDescriptor.typeParameters.size == unsubstitutedUnwrappedTypeParameters.size) {
                "Mismatching original / unwrapped type parameters: " +
                        "originalDescriptor: $originalDescriptor; " +
                        "unsubstitutedUnwrappedDescriptor: $unsubstitutedUnwrappedDescriptor"
            }

            if (unsubstitutedUnwrappedTypeParameters.isEmpty())
                null
            else
                unsubstitutedUnwrappedTypeParameters.associateWith {
                    val originalTypeParameter = candidateDescriptor.typeParameters[it.index]
                    val originalTypeArgument = originalTypeArguments[originalTypeParameter]
                        ?: throw AssertionError("No type argument for $originalTypeParameter")
                    originalTypeArgument
                }
        }

        is TypeAliasConstructorDescriptor -> {
            val substitutedType = originalDescriptor.returnType
            if (substitutedType.arguments.isEmpty())
                null
            else
                unsubstitutedUnwrappedTypeParameters.associateWith {
                    substitutedType.arguments[it.index].type
                }
        }

        else -> {
            if (originalTypeArguments.keys.all { it.containingDeclaration == unsubstitutedUnwrappedDescriptor })
                originalTypeArguments.takeIf { it.isNotEmpty() }
            else {
                assert(unsubstitutedUnwrappedTypeParameters.size == originalTypeArguments.size) {
                    "Mismatching type parameters and type arguments: " +
                            "unsubstitutedUnwrappedDescriptor: $unsubstitutedUnwrappedDescriptor; " +
                            "originalDescriptor: $originalDescriptor; " +
                            "originalTypeArguments: $originalTypeArguments"
                }

                if (unsubstitutedUnwrappedTypeParameters.isEmpty())
                    null
                else {
                    originalTypeArguments.keys.associate { originalTypeParameter ->
                        val unwrappedTypeParameter = unsubstitutedUnwrappedTypeParameters[originalTypeParameter.index]
                        val originalTypeArgument = originalTypeArguments[originalTypeParameter]
                            ?: throw AssertionError("No type argument for $unwrappedTypeParameter <= $originalTypeParameter")
                        unwrappedTypeParameter to originalTypeArgument
                    }
                }

            }
        }
    }

    val substitutedUnwrappedDescriptor =
        if (unwrappedTypeArguments == null)
            unwrappedDescriptor
        else {
            val substitutionContext = unsubstitutedUnwrappedDescriptor.typeParameters.associate {
                val typeArgument = unwrappedTypeArguments[it]
                    ?: throw AssertionError("No type argument for $it in $unwrappedTypeArguments")
                it.typeConstructor to TypeProjectionImpl(typeArgument)
            }
            unwrappedDescriptor.substitute(TypeSubstitutor.create(substitutionContext))
        }

    return CallBuilder(resolvedCall, substitutedUnwrappedDescriptor, unwrappedTypeArguments)
}

internal inline fun IrMemberAccessExpression<*>.putTypeArguments(
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    toIrType: (KotlinType) -> IrType
) {
    if (typeArguments == null) return
    for ((typeParameter, typeArgument) in typeArguments) {
        putTypeArgument(typeParameter.index, toIrType(typeArgument))
    }
}
