/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList

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

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    internal fun needToGenerateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrSimpleType,
        function: IrFunction
    ): Boolean =
        needSuspendConversion(type, function) || needCoercionToUnit(type, function) ||
                needVarargSpread(callableReferenceAccess, type, function)

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
        return expectedReturnType?.isUnit() == true && !function.returnType.isUnit()
    }

    /**
     * For example,
     * fun referenceConsumer(f: (Char, Char) -> String): String = ... // e.g., f(char1, char2)
     * fun referenced(vararg xs: Char) = ...
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { a, b -> referenced(a, b) }
     */
    private fun needVarargSpread(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrSimpleType,
        function: IrFunction
    ): Boolean {
        // Unbound callable reference 'A::foo'
        val shift = if (callableReferenceAccess.explicitReceiver is FirResolvedQualifier) 1 else 0
        val typeArguments = type.arguments
        // Drop the return type from type arguments
        val expectedParameterSize = typeArguments.size - 1 - shift
        if (expectedParameterSize < function.valueParameters.size) {
            return false
        }
        var hasSpreadCase = false
        function.valueParameters.forEachIndexed { index, irValueParameter ->
            if (irValueParameter.isVararg && typeArguments[shift + index] == irValueParameter.varargElementType) {
                hasSpreadCase = true
            }
        }
        return hasSpreadCase
    }

    internal fun ConeKotlinType.kFunctionTypeToFunctionType(): IrSimpleType {
        val kind =
            if (isSuspendFunctionType(session)) FunctionClassKind.SuspendFunction
            else FunctionClassKind.Function
        val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
        val coneType = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false)
        return coneType.toIrType() as IrSimpleType
    }

    internal fun generateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        adapteeSymbol: IrFunctionSymbol,
        type: IrSimpleType
    ): IrExpression {
        val firAdaptee = callableReferenceAccess.toResolvedCallableReference()?.resolvedSymbol?.fir as? FirSimpleFunction
        val adaptee = adapteeSymbol.owner
        val expectedReturnType = type.arguments.last().typeOrNull
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            val boundDispatchReceiver = callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = true)
            val boundExtensionReceiver = callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = false)

            val irAdapterFunction = createAdapterFunctionForCallableReference(
                callableReferenceAccess, startOffset, endOffset, firAdaptee!!, adaptee, type, boundDispatchReceiver, boundExtensionReceiver
            )
            val irCall = createAdapteeCallForCallableReference(
                callableReferenceAccess, adapteeSymbol, irAdapterFunction, boundDispatchReceiver, boundExtensionReceiver
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
                val irAdapterRef = IrFunctionReferenceImpl(
                    startOffset, endOffset, type, irAdapterFunction.symbol, irAdapterFunction.typeParameters.size,
                    irAdapterFunction.valueParameters.size, null, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
                )
                val statements = SmartList<IrStatement>()
                if (boundReceiver.isSafeToUseWithoutCopying()) {
                    irAdapterRef.extensionReceiver = boundReceiver
                } else {
                    val (irVariable, irVariableSymbol) = createTemporaryVariable(boundReceiver.deepCopyWithSymbols(), conversionScope)
                    irAdapterRef.extensionReceiver = IrGetValueImpl(startOffset, endOffset, irVariableSymbol)
                    statements.add(irVariable)
                }
                statements.add(irAdapterFunction)
                statements.add(irAdapterRef)

                IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE, statements)
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
        firAdaptee: FirSimpleFunction,
        adaptee: IrFunction,
        type: IrSimpleType,
        boundDispatchReceiver: IrExpression?,
        boundExtensionReceiver: IrExpression?
    ): IrSimpleFunction {
        val returnType = type.arguments.last().typeOrNull!!
        val parameterTypes = type.arguments.dropLast(1).map { it.typeOrNull!! }
        val adapterFunctionDescriptor = WrappedSimpleFunctionDescriptor()
        return symbolTable.declareSimpleFunction(adapterFunctionDescriptor) { irAdapterSymbol ->
            irFactory.createFunction(
                startOffset, endOffset,
                IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
                irAdapterSymbol,
                adaptee.name,
                DescriptorVisibilities.LOCAL,
                Modality.FINAL,
                returnType,
                isInline = firAdaptee.isInline,
                isExternal = firAdaptee.isExternal,
                isTailrec = firAdaptee.isTailRec,
                isSuspend = firAdaptee.isSuspend || type.isSuspendFunction(),
                isOperator = firAdaptee.isOperator,
                isInfix = firAdaptee.isInfix,
                isExpect = firAdaptee.isExpect,
                isFakeOverride = false
            ).also { irAdapterFunction ->
                adapterFunctionDescriptor.bind(irAdapterFunction)
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
                                index = -1,
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
    }

    private fun createAdapterParameter(
        adapterFunction: IrFunction,
        name: Name,
        index: Int,
        type: IrType,
        origin: IrDeclarationOrigin
    ): IrValueParameter {
        val startOffset = adapterFunction.startOffset
        val endOffset = adapterFunction.endOffset
        val descriptor = WrappedValueParameterDescriptor()
        return symbolTable.declareValueParameter(
            startOffset, endOffset, origin, descriptor, type
        ) { irAdapterParameterSymbol ->
            irFactory.createValueParameter(
                startOffset, endOffset,
                origin,
                irAdapterParameterSymbol,
                name,
                index,
                type,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).also { irAdapterValueParameter ->
                descriptor.bind(irAdapterValueParameter)
                irAdapterValueParameter.parent = adapterFunction
            }
        }
    }

    private fun IrValueDeclaration.toIrGetValue(startOffset: Int, endOffset: Int): IrGetValue =
        IrGetValueImpl(startOffset, endOffset, this.type, this.symbol)

    private fun createAdapteeCallForCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
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
        if (boundDispatchReceiver != null || boundExtensionReceiver != null) {
            val receiverValue = IrGetValueImpl(
                startOffset, endOffset, adapterFunction.extensionReceiverParameter!!.symbol, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )
            when {
                boundDispatchReceiver != null -> irCall.dispatchReceiver = receiverValue
                boundExtensionReceiver != null -> irCall.extensionReceiver = receiverValue
            }
        } else if (callableReferenceAccess.explicitReceiver is FirResolvedQualifier) {
            // Unbound callable reference 'A::foo'
            val adaptedReceiverParameter = adapterFunction.valueParameters[0]
            val adaptedReceiverValue = IrGetValueImpl(
                startOffset, endOffset, adaptedReceiverParameter.type, adaptedReceiverParameter.symbol
            )
            if (adapteeFunction.extensionReceiverParameter != null) {
                irCall.extensionReceiver = adaptedReceiverValue
                adapterParameterIndex++
            } else {
                irCall.dispatchReceiver = adaptedReceiverValue
            }
        }

        adapteeFunction.valueParameters.mapIndexed { index, valueParameter ->
            when {
                valueParameter.isVararg -> {
                    if (adapterFunction.valueParameters.size <= index) {
                        irCall.putValueArgument(index, null)
                    } else {
                        val adaptedValueArgument =
                            IrVarargImpl(startOffset, endOffset, valueParameter.type, valueParameter.varargElementType!!)
                        var neitherArrayNorSpread = false
                        while (adapterParameterIndex < adapterFunction.valueParameters.size) {
                            val irValueArgument =
                                adapterFunction.valueParameters[adapterParameterIndex].toIrGetValue(startOffset, endOffset)
                            if (irValueArgument.type == valueParameter.type) {
                                adaptedValueArgument.addElement(IrSpreadElementImpl(startOffset, endOffset, irValueArgument))
                                adapterParameterIndex++
                                break
                            } else if (irValueArgument.type == valueParameter.varargElementType) {
                                adaptedValueArgument.addElement(irValueArgument)
                                adapterParameterIndex++
                            } else {
                                neitherArrayNorSpread = true
                                break
                            }
                        }
                        if (neitherArrayNorSpread) {
                            irCall.putValueArgument(index, null)
                        } else {
                            irCall.putValueArgument(index, adaptedValueArgument)
                        }
                    }
                }
                valueParameter.hasDefaultValue() -> {
                    irCall.putValueArgument(index, null)
                }
                else -> {
                    irCall.putValueArgument(
                        index, adapterFunction.valueParameters[adapterParameterIndex++].toIrGetValue(startOffset, endOffset)
                    )
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
     * Instead of functions, a class with an overridden invoke can be used too:
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
        if (parameter?.returnTypeRef?.coneType?.isSuspendFunctionType(session) != true) {
            return this
        }

        val suspendConvertedType = parameter.returnTypeRef.toIrType() as IrSimpleType
        val returnType = suspendConvertedType.arguments.last().typeOrNull!!
        val invokeSymbol = findInvokeSymbol(suspendConvertedType, argument) ?: return this
        return argument.convertWithOffsets { startOffset, endOffset ->
            val irAdapterFunction = createAdapterFunctionForArgument(startOffset, endOffset, suspendConvertedType)
            // TODO: Should be able to reuse `this` if that is an immutable IrGetValue
            val irArgumentValue = createTemporaryVariable(this, conversionScope).first
            val irCall = createAdapteeCallForArgument(startOffset, endOffset, irAdapterFunction, invokeSymbol, irArgumentValue)
            irAdapterFunction.body = irFactory.createBlockBody(startOffset, endOffset) {
                if (returnType.isUnit()) {
                    statements.add(irCall)
                } else {
                    statements.add(IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, irAdapterFunction.symbol, irCall))
                }
            }

            val statements = SmartList<IrStatement>()
            statements.add(irArgumentValue)
            statements.add(
                IrFunctionExpressionImpl(
                    startOffset, endOffset, suspendConvertedType, irAdapterFunction, IrStatementOrigin.SUSPEND_CONVERSION
                )
            )
            IrBlockImpl(startOffset, endOffset, suspendConvertedType, IrStatementOrigin.SUSPEND_CONVERSION, statements)
        }
    }

    private fun findInvokeSymbol(type: IrSimpleType, argument: FirExpression): IrSimpleFunctionSymbol? {
        val argumentType = argument.typeRef.coneType
        // Either the argument type is a non-suspend functional type
        if (argumentType.isBuiltinFunctionalType(session) && !argumentType.isSuspendFunctionType(session)) {
            return (argument.typeRef.coneType as? ConeClassLikeType)?.lookupTag?.classId
                ?.let { classId ->
                    session.firSymbolProvider
                        .getClassDeclaredCallableSymbols(classId, Name.identifier("invoke"))
                        .filterIsInstance<FirFunctionSymbol<*>>()
                        .find { firFunctionSymbol ->
                            firFunctionSymbol.fir.valueParameters.size == type.arguments.size - 1
                        }?.let { firFunctionSymbol ->
                            declarationStorage.getIrFunctionSymbol(firFunctionSymbol) as? IrSimpleFunctionSymbol
                        }
                }
        }

        // Or is a class-like type whose corresponding class is a user-defined and has an overridden `invoke`.
        if (argumentType is ConeClassLikeType) {
            val klass =
                (session.firSymbolProvider.getClassLikeSymbolByFqName(argumentType.lookupTag.classId) as? FirRegularClassSymbol)?.fir
                    ?: return null
            if (klass.origin != FirDeclarationOrigin.Source) {
                return null
            }
            return klass.declarations
                .filterIsInstance<FirSimpleFunction>()
                .singleOrNull { it.name == Name.identifier("invoke") && it.valueParameters.size == type.arguments.size - 1 }
                ?.let { invokeFunction ->
                    declarationStorage.getIrFunctionSymbol(invokeFunction.symbol) as? IrSimpleFunctionSymbol
                }
        }

        return null
    }

    private fun createAdapterFunctionForArgument(
        startOffset: Int,
        endOffset: Int,
        type: IrSimpleType
    ): IrSimpleFunction {
        val returnType = type.arguments.last().typeOrNull!!
        val parameterTypes = type.arguments.dropLast(1).map { it.typeOrNull!! }
        val adapterFunctionDescriptor = WrappedSimpleFunctionDescriptor()
        return symbolTable.declareSimpleFunction(adapterFunctionDescriptor) { irAdapterSymbol ->
            irFactory.createFunction(
                startOffset, endOffset,
                IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION,
                irAdapterSymbol,
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
                adapterFunctionDescriptor.bind(irAdapterFunction)
                symbolTable.enterScope(irAdapterFunction)
                irAdapterFunction.valueParameters += parameterTypes.mapIndexed { index, parameterType ->
                    createAdapterParameter(
                        irAdapterFunction,
                        Name.identifier("p$index"),
                        index,
                        parameterType,
                        IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION
                    )
                }
                symbolTable.leaveScope(irAdapterFunction)
                irAdapterFunction.parent = conversionScope.parent()!!
            }
        }
    }

    private fun createAdapteeCallForArgument(
        startOffset: Int,
        endOffset: Int,
        adapterFunction: IrFunction,
        invokeSymbol: IrSimpleFunctionSymbol,
        irCapturedValue: IrValueDeclaration
    ): IrExpression {
        val irCall = IrCallImpl(
            startOffset, endOffset,
            adapterFunction.returnType,
            invokeSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = adapterFunction.valueParameters.size
        )
        irCall.dispatchReceiver = irCapturedValue.toIrGetValue(startOffset, endOffset)
        for (irAdapterParameter in adapterFunction.valueParameters) {
            irCall.putValueArgument(irAdapterParameter.index, irAdapterParameter.toIrGetValue(startOffset, endOffset))
        }
        return irCall
    }
}
