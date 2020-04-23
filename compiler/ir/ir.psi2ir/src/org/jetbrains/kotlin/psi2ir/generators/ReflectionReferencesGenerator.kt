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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.builtins.isKSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.CallBuilder
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.utils.SmartList

class ReflectionReferencesGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateClassLiteral(ktClassLiteral: KtClassLiteralExpression): IrExpression {
        val ktArgument = ktClassLiteral.receiverExpression!!
        val lhs = getOrFail(BindingContext.DOUBLE_COLON_LHS, ktArgument)
        val resultType = getTypeInferredByFrontendOrFail(ktClassLiteral).toIrType()

        return if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
            IrGetClassImpl(
                ktClassLiteral.startOffsetSkippingComments, ktClassLiteral.endOffset, resultType,
                ktArgument.genExpr()
            )
        } else {
            val typeConstructorDeclaration = lhs.type.constructor.declarationDescriptor
            val typeClass = typeConstructorDeclaration
                ?: throw AssertionError("Unexpected type constructor for ${lhs.type}: $typeConstructorDeclaration")
            IrClassReferenceImpl(
                ktClassLiteral.startOffsetSkippingComments, ktClassLiteral.endOffset, resultType,
                context.symbolTable.referenceClassifier(typeClass), lhs.type.toIrType()
            )
        }
    }

    fun generateCallableReference(ktCallableReference: KtCallableReferenceExpression): IrExpression {
        val resolvedCall = getResolvedCall(ktCallableReference.callableReference)!!
        val resolvedDescriptor = resolvedCall.resultingDescriptor

        val callBuilder = unwrapCallableDescriptorAndTypeArguments(resolvedCall, context.extensions.samConversion)

        val callableReferenceType = getTypeInferredByFrontendOrFail(ktCallableReference)
        if (resolvedCall.valueArguments.isNotEmpty() ||
            requiresCoercionToUnit(resolvedDescriptor, callableReferenceType) ||
            requiresSuspendConversion(resolvedDescriptor, callableReferenceType)
        ) {
            return generateAdaptedCallableReference(ktCallableReference, callBuilder, callableReferenceType)
        }

        return statementGenerator.generateCallReceiver(
            ktCallableReference,
            resolvedDescriptor,
            resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver,
            isSafe = false
        ).call { dispatchReceiverValue, extensionReceiverValue ->
            generateCallableReference(
                ktCallableReference,
                callableReferenceType,
                callBuilder.descriptor,
                callBuilder.typeArguments
            ).also { irCallableReference ->
                irCallableReference.dispatchReceiver = dispatchReceiverValue?.loadIfExists()
                irCallableReference.extensionReceiver = extensionReceiverValue?.loadIfExists()
            }
        }
    }

    private fun requiresCoercionToUnit(descriptor: CallableDescriptor, callableReferenceType: KotlinType): Boolean {
        val ktExpectedReturnType = callableReferenceType.arguments.last().type
        return KotlinBuiltIns.isUnit(ktExpectedReturnType) && !KotlinBuiltIns.isUnit(descriptor.returnType!!)
    }

    private fun requiresSuspendConversion(descriptor: CallableDescriptor, callableReferenceType: KotlinType): Boolean =
        descriptor is FunctionDescriptor &&
                !descriptor.isSuspend &&
                callableReferenceType.isKSuspendFunctionType

    private fun generateAdaptedCallableReference(
        ktCallableReference: KtCallableReferenceExpression,
        callBuilder: CallBuilder,
        callableReferenceType: KotlinType
    ): IrExpression {
        val adapteeDescriptor = callBuilder.descriptor
        if (adapteeDescriptor !is FunctionDescriptor) {
            throw AssertionError("Function descriptor expected in adapted callable reference: $adapteeDescriptor")
        }

        val startOffset = ktCallableReference.startOffsetSkippingComments
        val endOffset = ktCallableReference.endOffset

        val adapteeSymbol = context.symbolTable.referenceFunction(adapteeDescriptor.original)

        val ktFunctionalType = getTypeInferredByFrontendOrFail(ktCallableReference)
        val irFunctionalType = ktFunctionalType.maybeKFunctionTypeToFunctionType().toIrType()

        val ktFunctionalTypeArguments = ktFunctionalType.arguments
        val ktExpectedReturnType = ktFunctionalTypeArguments.last().type
        val ktExpectedParameterTypes = ktFunctionalTypeArguments.take(ktFunctionalTypeArguments.size - 1).map { it.type }

        val irAdapterFun =
            createAdapterFun(startOffset, endOffset, adapteeDescriptor, ktExpectedParameterTypes, ktExpectedReturnType, callBuilder, callableReferenceType)
        val irCall = createAdapteeCall(startOffset, endOffset, adapteeSymbol, callBuilder, irAdapterFun)

        irAdapterFun.body = IrBlockBodyImpl(startOffset, endOffset).apply {
            if (KotlinBuiltIns.isUnit(ktExpectedReturnType))
                statements.add(irCall)
            else
                statements.add(IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, irAdapterFun.symbol, irCall))
        }

        val resolvedCall = callBuilder.original
        return statementGenerator.generateCallReceiver(
            ktCallableReference,
            resolvedCall.resultingDescriptor,
            resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver,
            isSafe = false
        ).call { dispatchReceiverValue, extensionReceiverValue ->
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, irFunctionalType, irAdapterFun.symbol, irAdapterFun.typeParameters.size,
                irAdapterFun.valueParameters.size, null, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )

            val irDispatchReceiver = dispatchReceiverValue?.loadIfExists()
            val irExtensionReceiver = extensionReceiverValue?.loadIfExists()
            check(irDispatchReceiver == null || irExtensionReceiver == null) {
                "Bound callable reference cannot have both receivers: $adapteeDescriptor"
            }
            val receiver = irDispatchReceiver ?: irExtensionReceiver

            if (receiver == null) {
                IrFunctionExpressionImpl(
                    startOffset, endOffset, irFunctionalType, irAdapterFun, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
                )
            } else {
                val statements = SmartList<IrStatement>()
                if (receiver.isSafeToUseWithoutCopying()) {
                    irAdapterRef.extensionReceiver = receiver
                } else {
                    val irVariable = statementGenerator.scope.createTemporaryVariable(receiver, "receiver")
                    irAdapterRef.extensionReceiver = IrGetValueImpl(startOffset, endOffset, irVariable.symbol)
                    statements.add(irVariable)
                }
                statements.add(irAdapterFun)
                statements.add(irAdapterRef)

                IrBlockImpl(startOffset, endOffset, irFunctionalType, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE, statements)
            }
        }
    }

    private fun createAdapteeCall(
        startOffset: Int,
        endOffset: Int,
        adapteeSymbol: IrFunctionSymbol,
        callBuilder: CallBuilder,
        irAdapterFun: IrSimpleFunction
    ): IrExpression {
        val resolvedCall = callBuilder.original
        val resolvedDescriptor = resolvedCall.resultingDescriptor

        val irType = resolvedDescriptor.returnType!!.toIrType()

        val irCall =
            if (resolvedDescriptor is ConstructorDescriptor)
                IrConstructorCallImpl.fromSymbolDescriptor(
                    startOffset, endOffset, irType,
                    adapteeSymbol as IrConstructorSymbol
                )
            else
                IrCallImpl(
                    startOffset, endOffset, irType,
                    adapteeSymbol,
                    origin = null, superQualifierSymbol = null
                )

        val hasBoundDispatchReceiver = resolvedCall.dispatchReceiver != null && resolvedCall.dispatchReceiver !is TransientReceiver
        val hasBoundExtensionReceiver = resolvedCall.extensionReceiver != null && resolvedCall.extensionReceiver !is TransientReceiver
        if (hasBoundDispatchReceiver || hasBoundExtensionReceiver) {
            // In case of a bound reference, the receiver (which can only be one) is passed in the extension receiver parameter.
            val receiverValue = IrGetValueImpl(
                startOffset, endOffset, irAdapterFun.extensionReceiverParameter!!.symbol, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )
            when {
                hasBoundDispatchReceiver -> irCall.dispatchReceiver = receiverValue
                hasBoundExtensionReceiver -> irCall.extensionReceiver = receiverValue
            }
        }

        context.callToSubstitutedDescriptorMap[irCall] = resolvedDescriptor

        irCall.putTypeArguments(callBuilder.typeArguments) { it.toIrType() }

        putAdaptedValueArguments(startOffset, endOffset, irCall, irAdapterFun, resolvedCall)

        return irCall
    }

    private fun IrExpression.isSafeToUseWithoutCopying() =
        this is IrGetObjectValue ||
                this is IrGetEnumValue ||
                this is IrConst<*> ||
                this is IrGetValue && symbol.isBound && symbol.owner.isImmutable()

    private fun IrValueDeclaration.isImmutable() =
        this is IrValueParameter || this is IrVariable && !this.isVar

    private fun putAdaptedValueArguments(
        startOffset: Int,
        endOffset: Int,
        irAdapteeCall: IrFunctionAccessExpression,
        irAdapterFun: IrSimpleFunction,
        resolvedCall: ResolvedCall<*>
    ) {
        val adaptedArguments = resolvedCall.valueArguments
        var shift = 0
        if (resolvedCall.dispatchReceiver is TransientReceiver) {
            // Unbound callable reference 'A::foo', receiver is passed as a first parameter
            val irAdaptedReceiverParameter = irAdapterFun.valueParameters[0]
            irAdapteeCall.dispatchReceiver =
                IrGetValueImpl(startOffset, endOffset, irAdaptedReceiverParameter.type, irAdaptedReceiverParameter.symbol)
        } else if (resolvedCall.extensionReceiver is TransientReceiver) {
            val irAdaptedReceiverParameter = irAdapterFun.valueParameters[0]
            irAdapteeCall.extensionReceiver =
                IrGetValueImpl(startOffset, endOffset, irAdaptedReceiverParameter.type, irAdaptedReceiverParameter.symbol)
            shift = 1
        }

        for ((valueParameter, valueArgument) in adaptedArguments) {
            val substitutedValueParameter = resolvedCall.resultingDescriptor.valueParameters[valueParameter.index]
            irAdapteeCall.putValueArgument(
                valueParameter.index,
                adaptResolvedValueArgument(startOffset, endOffset, valueArgument, irAdapterFun, substitutedValueParameter, shift)
            )
        }
    }

    private fun adaptResolvedValueArgument(
        startOffset: Int,
        endOffset: Int,
        resolvedValueArgument: ResolvedValueArgument,
        irAdapterFun: IrSimpleFunction,
        valueParameter: ValueParameterDescriptor,
        shift: Int
    ): IrExpression? {
        return when (resolvedValueArgument) {
            is DefaultValueArgument ->
                null
            is VarargValueArgument ->
                if (resolvedValueArgument.arguments.isEmpty())
                    null
                else
                    IrVarargImpl(
                        startOffset, endOffset,
                        valueParameter.type.toIrType(), valueParameter.varargElementType!!.toIrType(),
                        resolvedValueArgument.arguments.map {
                            adaptValueArgument(startOffset, endOffset, it, irAdapterFun, shift)
                        }
                    )
            is ExpressionValueArgument -> {
                val valueArgument = resolvedValueArgument.valueArgument!!

                adaptValueArgument(startOffset, endOffset, valueArgument, irAdapterFun, shift) as IrExpression
            }
            else ->
                throw AssertionError("Unexpected ResolvedValueArgument: $resolvedValueArgument")
        }
    }

    private fun adaptValueArgument(
        startOffset: Int,
        endOffset: Int,
        valueArgument: ValueArgument,
        irAdapterFun: IrSimpleFunction,
        shift: Int
    ): IrVarargElement =
        when (valueArgument) {
            is FakeImplicitSpreadValueArgumentForCallableReference ->
                IrSpreadElementImpl(
                    startOffset, endOffset,
                    adaptValueArgument(startOffset, endOffset, valueArgument.expression, irAdapterFun, shift) as IrExpression
                )

            is FakePositionalValueArgumentForCallableReference -> {
                val irAdapterParameter = irAdapterFun.valueParameters[valueArgument.index + shift]
                IrGetValueImpl(startOffset, endOffset, irAdapterParameter.type, irAdapterParameter.symbol)
            }

            else ->
                throw AssertionError("Unexpected ValueArgument: $valueArgument")
        }

    private fun createAdapterFun(
        startOffset: Int,
        endOffset: Int,
        adapteeDescriptor: FunctionDescriptor,
        ktExpectedParameterTypes: List<KotlinType>,
        ktExpectedReturnType: KotlinType,
        callBuilder: CallBuilder,
        callableReferenceType: KotlinType
    ): IrSimpleFunction {
        val adapterFunctionDescriptor = WrappedSimpleFunctionDescriptor()

        val hasSuspendConversion = !adapteeDescriptor.isSuspend &&
                callableReferenceType.isKSuspendFunctionType

        return context.symbolTable.declareSimpleFunction(
            startOffset, endOffset,
            IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
            adapterFunctionDescriptor
        ) { irAdapterSymbol ->
            IrFunctionImpl(
                startOffset, endOffset,
                IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
                irAdapterSymbol,
                adapteeDescriptor.name,
                Visibilities.LOCAL,
                Modality.FINAL,
                ktExpectedReturnType.toIrType(),
                isInline = adapteeDescriptor.isInline, // TODO ?
                isExternal = false,
                isTailrec = false,
                isSuspend = adapteeDescriptor.isSuspend || hasSuspendConversion,
                isOperator = adapteeDescriptor.isOperator, // TODO ?
                isExpect = false,
                isFakeOverride = false
            ).also { irAdapterFun ->
                adapterFunctionDescriptor.bind(irAdapterFun)

                context.symbolTable.withScope(adapterFunctionDescriptor) {
                    irAdapterFun.metadata = MetadataSource.Function(adapteeDescriptor)

                    irAdapterFun.dispatchReceiverParameter = null

                    val boundReceiver = callBuilder.original.selectBoundReceiver()
                    if (boundReceiver != null) {
                        irAdapterFun.extensionReceiverParameter =
                            createAdapterParameter(startOffset, endOffset, Name.identifier("receiver"), -1, boundReceiver.type)
                    } else {
                        irAdapterFun.extensionReceiverParameter = null
                    }

                    irAdapterFun.valueParameters += ktExpectedParameterTypes.mapIndexed { index, ktExpectedParameterType ->
                        createAdapterParameter(startOffset, endOffset, Name.identifier("p$index"), index, ktExpectedParameterType)
                    }
                }
            }
        }
    }

    private fun ResolvedCall<*>.selectBoundReceiver(): ReceiverValue? {
        val dispatchReceiver = dispatchReceiver.takeUnless { it is TransientReceiver }
        val extensionReceiver = extensionReceiver.takeUnless { it is TransientReceiver }
        return when {
            dispatchReceiver == null -> extensionReceiver
            extensionReceiver == null -> dispatchReceiver
            else -> error("Bound callable references can't have both receivers: $resultingDescriptor")
        }
    }

    private fun createAdapterParameter(startOffset: Int, endOffset: Int, name: Name, index: Int, type: KotlinType): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        return context.symbolTable.declareValueParameter(
            startOffset, endOffset, IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE, descriptor, type.toIrType()
        ) { irAdapterParameterSymbol ->
            IrValueParameterImpl(
                startOffset, endOffset,
                IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                irAdapterParameterSymbol,
                name,
                index,
                type.toIrType(),
                varargElementType = null, isCrossinline = false, isNoinline = false
            ).also { irAdapterValueParameter ->
                descriptor.bind(irAdapterValueParameter)
            }
        }
    }

    fun generateCallableReference(
        ktElement: KtElement,
        type: KotlinType,
        callableDescriptor: CallableDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin? = null
    ): IrCallableReference {
        val startOffset = ktElement.startOffsetSkippingComments
        val endOffset = ktElement.endOffset
        return when (callableDescriptor) {
            is FunctionDescriptor -> {
                val symbol = context.symbolTable.referenceFunction(callableDescriptor.original)
                generateFunctionReference(startOffset, endOffset, type, symbol, callableDescriptor, typeArguments, origin)
            }
            is PropertyDescriptor -> {
                val mutable = get(BindingContext.VARIABLE, ktElement)?.isVar ?: true
                generatePropertyReference(startOffset, endOffset, type, callableDescriptor, typeArguments, origin, mutable)
            }
            else ->
                throw AssertionError("Unexpected callable reference: $callableDescriptor")
        }
    }

    fun generateLocalDelegatedPropertyReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        variableDescriptor: VariableDescriptorWithAccessors,
        irDelegateSymbol: IrVariableSymbol,
        origin: IrStatementOrigin?
    ): IrLocalDelegatedPropertyReference {
        val getterDescriptor =
            variableDescriptor.getter ?: throw AssertionError("Local delegated property should have a getter: $variableDescriptor")
        val setterDescriptor = variableDescriptor.setter

        val getterSymbol = context.symbolTable.referenceSimpleFunction(getterDescriptor)
        val setterSymbol = setterDescriptor?.let { context.symbolTable.referenceSimpleFunction(it) }

        return IrLocalDelegatedPropertyReferenceImpl(
            startOffset, endOffset, type.toIrType(),
            context.symbolTable.referenceLocalDelegatedProperty(variableDescriptor),
            irDelegateSymbol, getterSymbol, setterSymbol,
            origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = variableDescriptor
        }
    }

    private fun generatePropertyReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        propertyDescriptor: PropertyDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?,
        mutable: Boolean
    ): IrPropertyReference {
        val originalProperty = propertyDescriptor.original
        val originalGetter = originalProperty.getter?.original
        val originalSetter = if (mutable) originalProperty.setter?.original else null
        val originalSymbol = context.symbolTable.referenceProperty(originalProperty)

        return IrPropertyReferenceImpl(
            startOffset, endOffset, type.toIrType(),
            originalSymbol,
            if (typeArguments != null) propertyDescriptor.typeParametersCount else 0,
            getFieldForPropertyReference(originalProperty),
            originalGetter?.let { context.symbolTable.referenceSimpleFunction(it) },
            originalSetter?.let { context.symbolTable.referenceSimpleFunction(it) },
            origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = propertyDescriptor
            putTypeArguments(typeArguments) { it.toIrType() }
        }
    }

    @Suppress("DEPRECATION")
    private fun getFieldForPropertyReference(originalProperty: PropertyDescriptor) =
        // NB this is a hack, we really don't know if an arbitrary property has a backing field or not
        when {
            originalProperty.isDelegated -> null
            originalProperty.getter != null -> null
            else -> context.symbolTable.referenceField(originalProperty)
        }

    private fun generateFunctionReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?
    ): IrFunctionReference =
        IrFunctionReferenceImpl(
            startOffset, endOffset, type.toIrType(),
            symbol,
            typeArgumentsCount = descriptor.typeParametersCount,
            reflectionTarget = symbol,
            origin = origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = descriptor
            putTypeArguments(typeArguments) { it.toIrType() }
        }

    // This patches up a frontend bug -- adapted references are mistakenly given a KFunction type.
    private fun KotlinType.maybeKFunctionTypeToFunctionType() = when {
        isKFunctionType -> kFunctionTypeToFunctionType(false)
        isKSuspendFunctionType -> kFunctionTypeToFunctionType(true)
        else -> this
    }

    private fun KotlinType.kFunctionTypeToFunctionType(suspendFunction: Boolean) = createFunctionType(
        statementGenerator.context.builtIns,
        annotations,
        null,
        arguments.dropLast(1).map { it.type },
        null,
        arguments.last().type,
        suspendFunction
    )
}