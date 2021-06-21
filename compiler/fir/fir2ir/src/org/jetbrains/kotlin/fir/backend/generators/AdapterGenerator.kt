/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.FirFakeArgumentForCallableReference
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

/**
 * A generator that converts callable references or arguments that needs an adapter in between. This covers:
 *   1) Suspend conversion where a reference to or qualified access of non-suspend functional type is passed as an argument whose expected
 *     type is a suspend functional type;
 *   2) coercion-to-unit where a reference to a function whose return type isn't Unit is passed as an argument whose expected return type is
 *     Unit;
 *   3) vararg spread where a reference to a function with vararg parameter is passed as an argument whose use of that vararg parameter
 *     requires spreading.
 */
internal class AdapterGenerator(
    private val components: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    internal fun needToGenerateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrSimpleType,
        function: IrFunction
    ): Boolean =
        needSuspendConversion(type, function) || needCoercionToUnit(type, function) ||
                needVarargSpread(callableReferenceAccess)

    /**
     * For example,
     * fun referenceConsumer(f: suspend () -> Unit) = ...
     * fun nonSuspendFunction(...) = ...
     * fun useSite(...) = { ... referenceConsumer(::nonSuspendFunction) ... }
     *
     * At the use site, instead of referenced, we can put the suspend lambda as an adapter.
     */
    private fun needSuspendConversion(type: IrSimpleType, function: IrFunction): Boolean =
        // TODO: should refer to LanguageVersionSettings.SuspendConversion
        type.isKSuspendFunction() && !function.isSuspend

    /**
     * For example,
     * fun referenceConsumer(f: () -> Unit) = f()
     * fun referenced(...): Any { ... }
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { ... -> referenced(...) }
     */
    private fun needCoercionToUnit(type: IrSimpleType, function: IrFunction): Boolean {
        val expectedReturnType = type.arguments.last().typeOrNull
        val actualReturnType = function.returnType
        return expectedReturnType?.isUnit() == true &&
                // In case of an external function whose return type is a type parameter, e.g., operator fun <T, R> invoke(T): R
                !actualReturnType.isUnit() && !actualReturnType.isTypeParameter()
    }

    /**
     * For example,
     * fun referenceConsumer(f: (Char, Char) -> String): String = ... // e.g., f(char1, char2)
     * fun referenced(vararg xs: Char) = ...
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { a, b -> referenced(a, b) }
     */
    private fun needVarargSpread(callableReferenceAccess: FirCallableReferenceAccess): Boolean {
        // Unbound callable reference 'A::foo'
        return (callableReferenceAccess.calleeReference as? FirResolvedCallableReference)?.mappedArguments?.any {
            it.value is ResolvedCallArgument.VarargArgument || it.value is ResolvedCallArgument.DefaultArgument
        } == true
    }

    internal fun ConeKotlinType.kFunctionTypeToFunctionType(): IrSimpleType =
        kFunctionTypeToFunctionType(session).toIrType() as IrSimpleType

    internal fun generateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        adapteeSymbol: IrFunctionSymbol,
        type: IrSimpleType
    ): IrExpression {
        val firAdaptee = callableReferenceAccess.toResolvedCallableReference()?.resolvedSymbol?.fir as? FirFunction
        val adaptee = adapteeSymbol.owner
        val expectedReturnType = type.arguments.last().typeOrNull
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            val boundDispatchReceiver = callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = true)
            val boundExtensionReceiver = callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = false)

            val irAdapterFunction = createAdapterFunctionForCallableReference(
                callableReferenceAccess, startOffset, endOffset, firAdaptee!!, adaptee, type, boundDispatchReceiver, boundExtensionReceiver
            )
            val irCall = createAdapteeCallForCallableReference(
                callableReferenceAccess, firAdaptee, adapteeSymbol, irAdapterFunction, boundDispatchReceiver, boundExtensionReceiver
            )
            irAdapterFunction.body = irFactory.createBlockBody(startOffset, endOffset) {
                if (expectedReturnType?.isUnit() == true) {
                    statements.add(irCall)
                } else {
                    statements.add(IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, irAdapterFunction.symbol, irCall))
                }
            }

            val boundReceiver = boundDispatchReceiver ?: boundExtensionReceiver
            if (boundReceiver == null) {
                IrFunctionExpressionImpl(startOffset, endOffset, type, irAdapterFunction, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)
            } else {
                // TODO add a bound receiver property to IrFunctionExpressionImpl?
                val irAdapterRef = IrFunctionReferenceImpl(
                    startOffset, endOffset, type, irAdapterFunction.symbol, irAdapterFunction.typeParameters.size,
                    irAdapterFunction.valueParameters.size, null, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
                )
                IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE).apply {
                    statements.add(irAdapterFunction)
                    statements.add(irAdapterRef.apply { extensionReceiver = boundReceiver })
                }
            }
        }
    }

    private fun FirQualifiedAccess.findBoundReceiver(explicitReceiverExpression: IrExpression?, isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver is FirNoReceiverExpression) {
            return null
        }
        with(callGenerator) {
            return findIrReceiver(explicitReceiverExpression, isDispatch)
        }
    }

    private fun createAdapterFunctionForCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        startOffset: Int,
        endOffset: Int,
        firAdaptee: FirFunction<*>,
        adaptee: IrFunction,
        type: IrSimpleType,
        boundDispatchReceiver: IrExpression?,
        boundExtensionReceiver: IrExpression?
    ): IrSimpleFunction {
        val returnType = type.arguments.last().typeOrNull!!
        val parameterTypes = type.arguments.dropLast(1).map { it.typeOrNull!! }
        val firMemberAdaptee = firAdaptee as FirStatusOwner
        return irFactory.createFunction(
            startOffset, endOffset,
            IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
            IrSimpleFunctionSymbolImpl(),
            adaptee.name,
            DescriptorVisibilities.LOCAL,
            Modality.FINAL,
            returnType,
            isInline = firMemberAdaptee.isInline,
            isExternal = firMemberAdaptee.isExternal,
            isTailrec = firMemberAdaptee.isTailRec,
            isSuspend = firMemberAdaptee.isSuspend || type.isSuspendFunction(),
            isOperator = firMemberAdaptee.isOperator,
            isInfix = firMemberAdaptee.isInfix,
            isExpect = firMemberAdaptee.isExpect,
            isFakeOverride = false
        ).also { irAdapterFunction ->
            irAdapterFunction.metadata = FirMetadataSource.Function(firAdaptee)

            symbolTable.enterScope(irAdapterFunction)
            irAdapterFunction.dispatchReceiverParameter = null
            val boundReceiver = boundDispatchReceiver ?: boundExtensionReceiver
            when {
                boundReceiver == null ->
                    irAdapterFunction.extensionReceiverParameter = null
                boundDispatchReceiver != null && boundExtensionReceiver != null ->
                    error("Bound callable references can't have both receivers: ${callableReferenceAccess.render()}")
                else ->
                    irAdapterFunction.extensionReceiverParameter =
                        createAdapterParameter(
                            irAdapterFunction,
                            Name.identifier("receiver"),
                            index = UNDEFINED_PARAMETER_INDEX,
                            boundReceiver.type,
                            IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE
                        )
            }
            irAdapterFunction.valueParameters += parameterTypes.mapIndexed { index, parameterType ->
                createAdapterParameter(
                    irAdapterFunction,
                    Name.identifier("p$index"),
                    index,
                    parameterType,
                    IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE
                )
            }
            symbolTable.leaveScope(irAdapterFunction)

            irAdapterFunction.parent = conversionScope.parent()!!
        }
    }

    private fun createAdapterParameter(
        adapterFunction: IrFunction,
        name: Name,
        index: Int,
        type: IrType,
        origin: IrDeclarationOrigin
    ): IrValueParameter =
        irFactory.createValueParameter(
            adapterFunction.startOffset,
            adapterFunction.endOffset,
            origin,
            IrValueParameterSymbolImpl(),
            name,
            index,
            type,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
            isAssignable = false
        ).also { irAdapterValueParameter ->
            irAdapterValueParameter.parent = adapterFunction
        }

    private fun IrValueDeclaration.toIrGetValue(startOffset: Int, endOffset: Int): IrGetValue =
        IrGetValueImpl(startOffset, endOffset, this.type, this.symbol)

    private fun createAdapteeCallForCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        firAdaptee: FirFunction<*>,
        adapteeSymbol: IrFunctionSymbol,
        adapterFunction: IrFunction,
        boundDispatchReceiver: IrExpression?,
        boundExtensionReceiver: IrExpression?
    ): IrExpression {
        val adapteeFunction = adapteeSymbol.owner
        val startOffset = adapteeFunction.startOffset
        val endOffset = adapteeFunction.endOffset
        val type = adapteeFunction.returnType
        val irCall = when (adapteeSymbol) {
            is IrConstructorSymbol ->
                IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, adapteeSymbol)
            is IrSimpleFunctionSymbol ->
                IrCallImpl(
                    startOffset,
                    endOffset,
                    type,
                    adapteeSymbol,
                    callableReferenceAccess.typeArguments.size,
                    adapteeFunction.valueParameters.size,
                    origin = null,
                    superQualifierSymbol = null
                )
            else ->
                error("unknown callee kind: ${adapteeFunction.render()}")
        }

        var adapterParameterIndex = 0
        var parameterShift = 0
        if (boundDispatchReceiver != null || boundExtensionReceiver != null) {
            val receiverValue = IrGetValueImpl(
                startOffset, endOffset, adapterFunction.extensionReceiverParameter!!.symbol, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )
            when {
                boundDispatchReceiver != null -> irCall.dispatchReceiver = receiverValue
                boundExtensionReceiver != null -> irCall.extensionReceiver = receiverValue
            }
        } else if (callableReferenceAccess.explicitReceiver is FirResolvedQualifier && ((firAdaptee as? FirStatusOwner)?.isStatic != true)) {
            // Unbound callable reference 'A::foo'
            val adaptedReceiverParameter = adapterFunction.valueParameters[0]
            val adaptedReceiverValue = IrGetValueImpl(
                startOffset, endOffset, adaptedReceiverParameter.type, adaptedReceiverParameter.symbol
            )
            if (adapteeFunction.extensionReceiverParameter != null) {
                irCall.extensionReceiver = adaptedReceiverValue
            } else {
                irCall.dispatchReceiver = adaptedReceiverValue
            }
            parameterShift++
        }

        val mappedArguments = (callableReferenceAccess.calleeReference as? FirResolvedCallableReference)?.mappedArguments

        fun buildIrGetValueArgument(argument: FirExpression): IrGetValue {
            val parameterIndex = (argument as? FirFakeArgumentForCallableReference)?.index ?: adapterParameterIndex
            adapterParameterIndex++
            return adapterFunction.valueParameters[parameterIndex + parameterShift].toIrGetValue(startOffset, endOffset)
        }

        adapteeFunction.valueParameters.zip(firAdaptee.valueParameters).mapIndexed { index, (valueParameter, firParameter) ->
            when (val mappedArgument = mappedArguments?.get(firParameter)) {
                is ResolvedCallArgument.VarargArgument -> {
                    val valueArgument = if (mappedArgument.arguments.isEmpty()) {
                        null
                    } else {
                        val adaptedValueArgument = IrVarargImpl(
                            startOffset, endOffset,
                            valueParameter.type, valueParameter.varargElementType!!,
                        )
                        for (argument in mappedArgument.arguments) {
                            val irValueArgument = buildIrGetValueArgument(argument)
                            adaptedValueArgument.addElement(irValueArgument)
                        }
                        adaptedValueArgument
                    }
                    irCall.putValueArgument(index, valueArgument)
                }
                ResolvedCallArgument.DefaultArgument -> {
                    irCall.putValueArgument(index, null)
                }
                is ResolvedCallArgument.SimpleArgument -> {
                    val irValueArgument = buildIrGetValueArgument(mappedArgument.callArgument)
                    if (valueParameter.isVararg) {
                        irCall.putValueArgument(
                            index, IrVarargImpl(
                                startOffset, endOffset,
                                valueParameter.type, valueParameter.varargElementType!!,
                                listOf(IrSpreadElementImpl(startOffset, endOffset, irValueArgument))
                            )
                        )
                    } else {
                        irCall.putValueArgument(index, irValueArgument)
                    }
                }
                null -> {
                }
            }
        }

        with(callGenerator) {
            return irCall.applyTypeArguments(callableReferenceAccess)
        }
    }

    /**
     * For example,
     * fun consumer(f: suspend () -> Unit) = ...
     * fun nonSuspendFunction = { ... }
     * fun useSite(...) = { ... consumer(nonSuspendFunction) ... }
     *
     * At the use site, instead of the argument, we can put the suspend lambda as an adapter.
     *
     * Instead of functions, a subtype of functional type can be used too:
     * class Foo {
     *   override fun invoke() = ...
     * }
     * fun useSite(...) = { ... consumer(Foo()) ... }
     */
    internal fun IrExpression.applySuspendConversionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?
    ): IrExpression {
        // TODO: should refer to LanguageVersionSettings.SuspendConversion
        if (this is IrBlock && origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) {
            return this
        }
        val expectedType = parameter?.returnTypeRef?.coneType ?: return this
        // Expect the expected type to be a suspend functional type.
        if (!expectedType.isSuspendFunctionType(session)) {
            return this
        }
        val expectedFunctionalType = expectedType.suspendFunctionTypeToFunctionType(session)

        val invokeSymbol = findInvokeSymbol(expectedFunctionalType, argument) ?: return this
        val suspendConvertedType = expectedType.toIrType() as IrSimpleType
        return argument.convertWithOffsets { startOffset, endOffset ->
            val irAdapterFunction = createAdapterFunctionForArgument(startOffset, endOffset, suspendConvertedType, type, invokeSymbol)
            // TODO add a bound receiver property to IrFunctionExpressionImpl?
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, suspendConvertedType, irAdapterFunction.symbol, irAdapterFunction.typeParameters.size,
                irAdapterFunction.valueParameters.size, null, IrStatementOrigin.SUSPEND_CONVERSION
            )
            IrBlockImpl(startOffset, endOffset, suspendConvertedType, IrStatementOrigin.SUSPEND_CONVERSION).apply {
                statements.add(irAdapterFunction)
                statements.add(irAdapterRef.apply { extensionReceiver = this@applySuspendConversionIfNeeded })
            }
        }
    }

    private fun findInvokeSymbol(expectedFunctionalType: ConeClassLikeType, argument: FirExpression): IrSimpleFunctionSymbol? {
        val argumentType = argument.typeRef.coneType
        val argumentTypeWithInvoke = argumentType.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType) ?: return null

        return if (argumentTypeWithInvoke.isBuiltinFunctionalType(session)) {
            (argumentTypeWithInvoke as? ConeClassLikeType)?.findBaseInvokeSymbol(session, scopeSession)
        } else {
            argumentTypeWithInvoke.findContributedInvokeSymbol(
                session, scopeSession, expectedFunctionalType, shouldCalculateReturnTypesOfFakeOverrides = true
            )
        }?.let {
            declarationStorage.getIrFunctionSymbol(it) as? IrSimpleFunctionSymbol
        }
    }

    private fun createAdapterFunctionForArgument(
        startOffset: Int,
        endOffset: Int,
        type: IrSimpleType,
        argumentType: IrType,
        invokeSymbol: IrSimpleFunctionSymbol
    ): IrSimpleFunction {
        val returnType = type.arguments.last().typeOrNull!!
        val parameterTypes = type.arguments.dropLast(1).map { it.typeOrNull!! }
        return irFactory.createFunction(
            startOffset, endOffset,
            IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION,
            IrSimpleFunctionSymbolImpl(),
            // TODO: need a better way to avoid name clash
            Name.identifier("suspendConversion"),
            DescriptorVisibilities.LOCAL,
            Modality.FINAL,
            returnType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = true,
            isOperator = false,
            isInfix = false,
            isExpect = false,
            isFakeOverride = false
        ).also { irAdapterFunction ->
            symbolTable.enterScope(irAdapterFunction)
            irAdapterFunction.extensionReceiverParameter = createAdapterParameter(
                irAdapterFunction,
                Name.identifier("callee"),
                UNDEFINED_PARAMETER_INDEX,
                argumentType,
                IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION
            )
            irAdapterFunction.valueParameters += parameterTypes.mapIndexed { index, parameterType ->
                createAdapterParameter(
                    irAdapterFunction,
                    Name.identifier("p$index"),
                    index,
                    parameterType,
                    IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION
                )
            }
            irAdapterFunction.body = irFactory.createBlockBody(startOffset, endOffset) {
                val irCall = createAdapteeCallForArgument(startOffset, endOffset, irAdapterFunction, invokeSymbol)
                if (returnType.isUnit()) {
                    statements.add(irCall)
                } else {
                    statements.add(IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, irAdapterFunction.symbol, irCall))
                }
            }
            symbolTable.leaveScope(irAdapterFunction)
            irAdapterFunction.parent = conversionScope.parent()!!
        }
    }

    private fun createAdapteeCallForArgument(
        startOffset: Int,
        endOffset: Int,
        adapterFunction: IrFunction,
        invokeSymbol: IrSimpleFunctionSymbol
    ): IrExpression {
        val irCall = IrCallImpl(
            startOffset, endOffset,
            adapterFunction.returnType,
            invokeSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = adapterFunction.valueParameters.size
        )
        irCall.dispatchReceiver = adapterFunction.extensionReceiverParameter!!.toIrGetValue(startOffset, endOffset)
        for (irAdapterParameter in adapterFunction.valueParameters) {
            irCall.putValueArgument(irAdapterParameter.index, irAdapterParameter.toIrGetValue(startOffset, endOffset))
        }
        return irCall
    }
}
