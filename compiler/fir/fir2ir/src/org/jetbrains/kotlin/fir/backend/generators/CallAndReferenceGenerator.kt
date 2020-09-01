/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.isFunctional
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi2ir.generators.hasNoSideEffects
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities

class CallAndReferenceGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    fun convertToIrCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?
    ): IrExpression {
        val symbol = callableReferenceAccess.calleeReference.toSymbol(session, classifierStorage, declarationStorage, conversionScope)
        val type = callableReferenceAccess.typeRef.toIrType()
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrPropertySymbol -> {
                    val referencedProperty = symbol.owner
                    val referencedPropertyGetter = referencedProperty.getter
                    val backingFieldSymbol = when {
                        referencedPropertyGetter != null -> null
                        else -> referencedProperty.backingField?.symbol
                    }
                    val origin = when (callableReferenceAccess.source?.psi?.parent) {
                        is KtPropertyDelegate -> IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
                        else -> null
                    }
                    IrPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = referencedPropertyGetter?.typeParameters?.size ?: 0,
                        backingFieldSymbol,
                        referencedPropertyGetter?.symbol,
                        referencedProperty.setter?.symbol,
                        origin
                    )
                }
                is IrLocalDelegatedPropertySymbol -> {
                    IrLocalDelegatedPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        symbol.owner.delegate.symbol,
                        symbol.owner.getter.symbol as IrSimpleFunctionSymbol,
                        symbol.owner.setter?.symbol as IrSimpleFunctionSymbol?,
                        IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
                    )
                }
                is IrConstructorSymbol -> {
                    val constructor = symbol.owner
                    val klass = constructor.parent as? IrClass
                    IrFunctionReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = constructor.typeParameters.size + (klass?.typeParameters?.size ?: 0),
                        valueArgumentsCount = constructor.valueParameters.size,
                        reflectionTarget = symbol
                    )
                }
                is IrFunctionSymbol -> {
                    assert(type.isFunctionTypeOrSubtype()) {
                        "Callable reference whose symbol refers to a function should be of functional type."
                    }
                    type as IrSimpleType
                    val function = symbol.owner
                    // TODO: should refer to LanguageVersionSettings.SuspendConversion
                    if (requiresVarargSpread(callableReferenceAccess, type, function) ||
                        requiresCoercionToUnit(type, function) ||
                        requiresSuspendConversion(type, function)
                    ) {
                        val adaptedType = callableReferenceAccess.typeRef.coneType.kFunctionTypeToFunctionType()
                        generateAdaptedCallableReference(callableReferenceAccess, explicitReceiverExpression, symbol, adaptedType)
                    } else {
                        IrFunctionReferenceImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = function.typeParameters.size,
                            valueArgumentsCount = function.valueParameters.size,
                            reflectionTarget = symbol
                        )
                    }
                }
                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
                    )
                }
            }
        }.applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
    }

    /*
     * For example,
     * fun referenceConsumer(f: (Char, Char) -> String): String = ... // e.g., f(char1, char2)
     * fun referenced(vararg xs: Char) = ...
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { a, b -> referenced(a, b) }
     */
    private fun requiresVarargSpread(
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

    /*
     * For example,
     * fun referenceConsumer(f: () -> Unit) = f()
     * fun referenced(...): Any { ... }
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { ... -> referenced(...) }
     */
    private fun requiresCoercionToUnit(type: IrSimpleType, function: IrFunction): Boolean {
        val expectedReturnType = type.arguments.last().typeOrNull
        return expectedReturnType?.isUnit() == true && !function.returnType.isUnit()
    }

    /*
     * For example,
     * fun referenceConsumer(f: suspend () -> Unit) = ...
     * fun nonSuspendFunction(...) = ...
     * fun useSite(...) = { ... referenceConsumer(::nonSuspendFunction) ... }
     *
     * At the use site, instead of referenced, we can put the suspend lambda as an adapter.
     */
    private fun requiresSuspendConversion(type: IrSimpleType, function: IrFunction): Boolean =
        type.isKSuspendFunction() && !function.isSuspend

    private fun ConeKotlinType.kFunctionTypeToFunctionType(): IrSimpleType {
        val kind =
            if (isSuspendFunctionType(session)) FunctionClassKind.SuspendFunction
            else FunctionClassKind.Function
        val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
        val coneType = ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false)
        return coneType.toIrType() as IrSimpleType
    }

    private fun generateAdaptedCallableReference(
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

            val irAdapterFunction = createAdapterFunction(
                callableReferenceAccess, startOffset, endOffset, firAdaptee!!, adaptee, type, boundDispatchReceiver, boundExtensionReceiver
            )
            val irCall = createAdapteeCall(
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
                    val (irVariable, irVariableSymbol) =
                        createTemporaryVariableForSafeCallConstruction(boundReceiver.deepCopyWithSymbols(), conversionScope)
                    irAdapterRef.extensionReceiver = IrGetValueImpl(startOffset, endOffset, irVariableSymbol)
                    statements.add(irVariable)
                }
                statements.add(irAdapterFunction)
                statements.add(irAdapterRef)

                IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE, statements)
            }
        }
    }

    private fun createAdapterFunction(
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
                            createAdapterParameter(irAdapterFunction, Name.identifier("receiver"), -1, boundReceiver.type)
                }
                irAdapterFunction.valueParameters += parameterTypes.mapIndexed { index, parameterType ->
                    createAdapterParameter(irAdapterFunction, Name.identifier("p$index"), index, parameterType)
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
        type: IrType
    ): IrValueParameter {
        val startOffset = adapterFunction.startOffset
        val endOffset = adapterFunction.endOffset
        val descriptor = WrappedValueParameterDescriptor()
        return symbolTable.declareValueParameter(
            startOffset, endOffset, IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE, descriptor, type
        ) { irAdapterParameterSymbol ->
            irFactory.createValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
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

    private fun createAdapteeCall(
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

        fun IrValueParameter.toGetValue(): IrGetValue =
            IrGetValueImpl(startOffset, endOffset, this.type, this.symbol)

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
                            val irValueArgument = adapterFunction.valueParameters[adapterParameterIndex].toGetValue()
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
                    irCall.putValueArgument(index, adapterFunction.valueParameters[adapterParameterIndex++].toGetValue())
                }
            }
        }

        return irCall.applyTypeArguments(callableReferenceAccess)
    }

    private fun FirQualifiedAccess.tryConvertToSamConstructorCall(type: IrType): IrTypeOperatorCall? {
        val calleeReference = calleeReference as? FirResolvedNamedReference ?: return null
        val fir = calleeReference.resolvedSymbol.fir
        if (this is FirFunctionCall && fir is FirSimpleFunction && fir.origin == FirDeclarationOrigin.SamConstructor) {
            return convertWithOffsets { startOffset, endOffset ->
                IrTypeOperatorCallImpl(
                    startOffset, endOffset, type, IrTypeOperator.SAM_CONVERSION, type, visitor.convertToIrExpression(argument)
                )
            }
        }
        return null
    }

    private fun FirExpression.superQualifierSymbol(callSymbol: IrSymbol?): IrClassSymbol? {
        if (this !is FirQualifiedAccess) {
            return null
        }
        val dispatchReceiverReference = calleeReference
        if (dispatchReceiverReference !is FirSuperReference) {
            return null
        }
        val superTypeRef = dispatchReceiverReference.superTypeRef
        val coneSuperType = superTypeRef.coneTypeSafe<ConeClassLikeType>()
        if (coneSuperType != null) {
            val firClassSymbol = coneSuperType.lookupTag.toSymbol(session) as? FirClassSymbol<*>
            if (firClassSymbol != null) {
                return classifierStorage.getIrClassSymbol(firClassSymbol)
            }
        } else if (superTypeRef is FirComposedSuperTypeRef) {
            val owner = callSymbol?.owner
            if (owner != null && owner is IrDeclaration) {
                return owner.parentClassOrNull?.symbol
            }
        }
        return null
    }

    fun convertToIrCall(
        qualifiedAccess: FirQualifiedAccess,
        typeRef: FirTypeRef,
        explicitReceiverExpression: IrExpression?,
        annotationMode: Boolean = false
    ): IrExpression {
        try {
            val type = typeRef.toIrType()
            val samConstructorCall = qualifiedAccess.tryConvertToSamConstructorCall(type)
            if (samConstructorCall != null) return samConstructorCall

            val symbol = qualifiedAccess.calleeReference.toSymbol(
                session,
                classifierStorage,
                declarationStorage,
                conversionScope
            )
            return qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
                val dispatchReceiver = qualifiedAccess.dispatchReceiver
                if (qualifiedAccess.calleeReference is FirSuperReference) {
                    if (dispatchReceiver !is FirNoReceiverExpression) {
                        return@convertWithOffsets visitor.convertToIrExpression(dispatchReceiver)
                    }
                }
                when (symbol) {
                    is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                    is IrSimpleFunctionSymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = symbol.owner.typeParameters.size,
                            valueArgumentsCount = symbol.owner.valueParameters.size,
                            origin = qualifiedAccess.calleeReference.statementOrigin(),
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                        )
                    }
                    is IrLocalDelegatedPropertySymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol.owner.getter.symbol as IrSimpleFunctionSymbol,
                            typeArgumentsCount = symbol.owner.getter.typeParameters.size,
                            valueArgumentsCount = 0,
                            origin = IrStatementOrigin.GET_LOCAL_PROPERTY,
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                        )
                    }
                    is IrPropertySymbol -> {
                        val getter = symbol.owner.getter
                        val backingField = symbol.owner.backingField
                        when {
                            getter != null -> IrCallImpl(
                                startOffset, endOffset, type, getter.symbol,
                                typeArgumentsCount = getter.typeParameters.size,
                                valueArgumentsCount = 0,
                                origin = IrStatementOrigin.GET_PROPERTY,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                            )
                            backingField != null -> IrGetFieldImpl(
                                startOffset, endOffset, backingField.symbol, type,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                            )
                            else -> IrErrorCallExpressionImpl(
                                startOffset, endOffset, type,
                                description = "No getter or backing field found for ${qualifiedAccess.calleeReference.render()}"
                            )
                        }
                    }
                    is IrFieldSymbol -> IrGetFieldImpl(
                        startOffset, endOffset, symbol, type,
                        origin = IrStatementOrigin.GET_PROPERTY.takeIf { qualifiedAccess.calleeReference !is FirDelegateFieldReference },
                        superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                    )
                    is IrValueSymbol -> IrGetValueImpl(
                        startOffset, endOffset, type, symbol,
                        origin = qualifiedAccess.calleeReference.statementOrigin()
                    )
                    is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                    else -> generateErrorCallExpression(startOffset, endOffset, qualifiedAccess.calleeReference, type)
                }
            }.applyCallArguments(qualifiedAccess as? FirCall, annotationMode)
                .applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, explicitReceiverExpression)
        } catch (e: Throwable) {
            throw IllegalStateException("Error while translating ${qualifiedAccess.render()} " +
                                                "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e)
        }
    }

    fun convertToIrSetCall(variableAssignment: FirVariableAssignment, explicitReceiverExpression: IrExpression?): IrExpression {
        val type = irBuiltIns.unitType
        val calleeReference = variableAssignment.calleeReference
        val symbol = calleeReference.toSymbol(session, classifierStorage, declarationStorage, conversionScope, preferGetter = false)
        val origin = IrStatementOrigin.EQ
        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            val assignedValue = visitor.convertToIrExpression(variableAssignment.rValue)
            when (symbol) {
                is IrFieldSymbol -> IrSetFieldImpl(startOffset, endOffset, symbol, type, origin).apply {
                    value = assignedValue
                }
                is IrLocalDelegatedPropertySymbol -> {
                    val setter = symbol.owner.setter
                    when {
                        setter != null -> IrCallImpl(
                            startOffset, endOffset, type, setter.symbol as IrSimpleFunctionSymbol,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                        else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                    }
                }
                is IrPropertySymbol -> {
                    val irProperty = symbol.owner
                    val setter = irProperty.setter
                    val backingField = irProperty.backingField
                    when {
                        setter != null -> IrCallImpl(
                            startOffset, endOffset, type, setter.symbol,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                        backingField != null -> IrSetFieldImpl(
                            startOffset, endOffset, backingField.symbol, type,
                            origin = null, // NB: to be consistent with PSI2IR, origin should be null here
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
                        ).apply {
                            value = assignedValue
                        }
                        else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                    }
                }
                is IrSimpleFunctionSymbol -> {
                    IrCallImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = symbol.owner.typeParameters.size,
                        valueArgumentsCount = 1,
                        origin = origin
                    ).apply {
                        putValueArgument(0, assignedValue)
                    }
                }
                is IrVariableSymbol -> IrSetValueImpl(startOffset, endOffset, type, symbol, assignedValue, origin)
                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.applyTypeArguments(variableAssignment).applyReceivers(variableAssignment, explicitReceiverExpression)
    }

    fun convertToIrConstructorCall(annotationCall: FirAnnotationCall): IrExpression {
        val coneType = annotationCall.annotationTypeRef.coneTypeSafe<ConeLookupTagBasedType>()
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        return annotationCall.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val irConstructor = (annotationCall.toResolvedCallableSymbol() as? FirConstructorSymbol)?.let {
                        this.declarationStorage.getIrConstructorSymbol(it)
                    } ?: run {
                        // Fallback for FirReferencePlaceholderForResolvedAnnotations from jar
                        val fir = coneType.lookupTag.toSymbol(session)?.fir as? FirClass<*>
                        var constructorSymbol: FirConstructorSymbol? = null
                        fir?.unsubstitutedScope(session, scopeSession)?.processDeclaredConstructors {
                            if (it.fir.isPrimary && constructorSymbol == null) {
                                constructorSymbol = it
                            }
                        }
                        constructorSymbol?.let {
                            this.declarationStorage.getIrConstructorSymbol(it)
                        }
                    }
                    if (irConstructor == null) {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No annotation constructor found: ${irClass.name}")
                    } else {
                        IrConstructorCallImpl(
                            startOffset, endOffset, type, irConstructor,
                            valueArgumentsCount = irConstructor.owner.valueParameters.size,
                            typeArgumentsCount = 0,
                            constructorTypeArgumentsCount = 0
                        )
                    }

                }
                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset,
                        endOffset,
                        type ?: createErrorType(),
                        "Unresolved reference: ${annotationCall.render()}"
                    )
                }
            }
        }.applyCallArguments(annotationCall, annotationMode = true)
    }

    fun convertToGetObject(qualifier: FirResolvedQualifier): IrExpression {
        return convertToGetObject(qualifier, callableReferenceMode = false)!!
    }

    internal fun convertToGetObject(qualifier: FirResolvedQualifier, callableReferenceMode: Boolean): IrExpression? {
        val classSymbol = (qualifier.typeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)
        if (callableReferenceMode && classSymbol is FirRegularClassSymbol) {
            if (classSymbol.classId != qualifier.classId) {
                return null
            }
        }
        val irType = qualifier.typeRef.toIrType()
        return qualifier.convertWithOffsets { startOffset, endOffset ->
            if (classSymbol != null) {
                IrGetObjectValueImpl(
                    startOffset, endOffset, irType,
                    classSymbol.toSymbol(this.session, this.classifierStorage) as IrClassSymbol
                )
            } else {
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, irType,
                    "Resolved qualifier ${qualifier.render()} does not have correctly resolved type"
                )
            }
        }
    }

    internal fun IrExpression.applyCallArguments(call: FirCall?, annotationMode: Boolean): IrExpression {
        if (call == null) return this
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        val calleeReference = when (call) {
                            is FirFunctionCall -> call.calleeReference
                            is FirDelegatedConstructorCall -> call.calleeReference
                            is FirAnnotationCall -> call.calleeReference
                            else -> null
                        } as? FirResolvedNamedReference
                        val function = (calleeReference?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
                        val valueParameters = function?.valueParameters
                        val argumentMapping = call.argumentMapping
                        if (argumentMapping != null && (annotationMode || argumentMapping.isNotEmpty())) {
                            if (valueParameters != null) {
                                return applyArgumentsWithReorderingIfNeeded(argumentMapping, valueParameters, annotationMode)
                            }
                        }
                        for ((index, argument) in call.arguments.withIndex()) {
                            val valueParameter = valueParameters?.get(index)
                            val argumentExpression =
                                visitor.convertToIrExpression(argument)
                                    .applySamConversionIfNeeded(argument, valueParameter)
                                    .applyAssigningArrayElementsToVarargInNamedForm(argument, valueParameter)
                            putValueArgument(index, argumentExpression)
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount arguments to $name call with $valueArgumentsCount parameters"
                    ).apply {
                        for (argument in call.arguments) {
                            addArgument(visitor.convertToIrExpression(argument))
                        }
                    }
                }
            }
            is IrErrorCallExpressionImpl -> apply {
                for (argument in call.arguments) {
                    addArgument(visitor.convertToIrExpression(argument))
                }
            }
            else -> this
        }
    }

    private fun IrMemberAccessExpression<*>.applyArgumentsWithReorderingIfNeeded(
        argumentMapping: Map<FirExpression, FirValueParameter>,
        valueParameters: List<FirValueParameter>,
        annotationMode: Boolean
    ): IrExpression {
        // Assuming compile-time constants only inside annotation, we don't need a block to reorder arguments to preserve semantics.
        // But, we still need to pick correct indices for named arguments.
        if (!annotationMode &&
            needArgumentReordering(argumentMapping.values, valueParameters)
        ) {
            return IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL).apply {
                for ((argument, parameter) in argumentMapping) {
                    val parameterIndex = valueParameters.indexOf(parameter)
                    val irArgument =
                        visitor.convertToIrExpression(argument)
                            .applySamConversionIfNeeded(argument, parameter)
                            .applyAssigningArrayElementsToVarargInNamedForm(argument, parameter)
                    if (irArgument.hasNoSideEffects()) {
                        putValueArgument(parameterIndex, irArgument)
                    } else {
                        val tempVar = declarationStorage.declareTemporaryVariable(irArgument, parameter.name.asString()).apply {
                            parent = conversionScope.parentFromStack()
                        }
                        this.statements.add(tempVar)
                        putValueArgument(parameterIndex, IrGetValueImpl(startOffset, endOffset, tempVar.symbol, null))
                    }
                }
                this.statements.add(this@applyArgumentsWithReorderingIfNeeded)
            }
        } else {
            for ((argument, parameter) in argumentMapping) {
                val argumentExpression =
                    visitor.convertToIrExpression(argument, annotationMode)
                        .applySamConversionIfNeeded(argument, parameter)
                        .applyAssigningArrayElementsToVarargInNamedForm(argument, parameter)
                putValueArgument(valueParameters.indexOf(parameter), argumentExpression)
            }
            if (annotationMode) {
                for ((index, parameter) in valueParameters.withIndex()) {
                    if (parameter.isVararg && !argumentMapping.containsValue(parameter)) {
                        val elementType = parameter.returnTypeRef.toIrType()
                        putValueArgument(
                            index, IrVarargImpl(-1, -1, elementType, elementType.toArrayOrPrimitiveArrayType(irBuiltIns))
                        )
                    }
                }
            }
            return this
        }
    }

    private fun needArgumentReordering(
        parametersInActualOrder: Collection<FirValueParameter>,
        valueParameters: List<FirValueParameter>
    ): Boolean {
        var lastValueParameterIndex = -1
        for (parameter in parametersInActualOrder) {
            val index = valueParameters.indexOf(parameter)
            if (index < lastValueParameterIndex) {
                return true
            }
            lastValueParameterIndex = index
        }
        return false
    }

    private fun IrExpression.applySamConversionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?
    ): IrExpression {
        if (parameter == null || !needSamConversion(argument, parameter)) {
            return this
        }
        val samType = parameter.returnTypeRef.toIrType()
        // Make sure the converted IrType owner indeed has a single abstract method, since FunctionReferenceLowering relies on it.
        if (!samType.isSamType) return this
        return IrTypeOperatorCallImpl(this.startOffset, this.endOffset, samType, IrTypeOperator.SAM_CONVERSION, samType, this)
    }

    private fun needSamConversion(argument: FirExpression, parameter: FirValueParameter): Boolean {
        // If the expected type is a built-in functional type, we don't need SAM conversion.
        if (parameter.returnTypeRef.coneType.isBuiltinFunctionalType(session)) {
            return false
        }
        // On the other hand, the actual type should be a functional type.
        return argument.isFunctional(session)
    }

    private fun IrExpression.applyAssigningArrayElementsToVarargInNamedForm(
        argument: FirExpression,
        parameter: FirValueParameter?
    ): IrExpression {
        // TODO: Need to refer to language feature: AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
        if (this !is IrVarargImpl ||
            parameter?.isVararg != true ||
            argument !is FirVarargArgumentsExpression ||
            argument.arguments.none { it is FirNamedArgumentExpression }
        ) {
            return this
        }
        elements.forEachIndexed { i, irVarargElement ->
            if (irVarargElement !is IrSpreadElement &&
                argument.arguments[i] is FirNamedArgumentExpression &&
                irVarargElement is IrExpression &&
                irVarargElement.type.isArray()
            ) {
                elements[i] = IrSpreadElementImpl(irVarargElement.startOffset, irVarargElement.endOffset, irVarargElement)
            }
        }
        return this
    }

    private fun IrExpression.applyTypeArguments(access: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val argumentsCount = access.typeArguments.size
                if (argumentsCount <= typeArgumentsCount) {
                    apply {
                        for ((index, argument) in access.typeArguments.withIndex()) {
                            val argumentIrType = (argument as FirTypeProjectionWithVariance).typeRef.toIrType()
                            putTypeArgument(index, argumentIrType)
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount type arguments to $name call with $typeArgumentsCount type parameters"
                    )
                }
            }
            is IrBlockImpl -> apply {
                if (statements.isNotEmpty()) {
                    val lastStatement = statements.last()
                    if (lastStatement is IrExpression) {
                        statements[statements.size - 1] = lastStatement.applyTypeArguments(access)
                    }
                }
            }
            else -> this
        }
    }

    private fun FirQualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = true)

    private fun FirQualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = false)

    private fun FirQualifiedAccess.findIrReceiver(explicitReceiverExpression: IrExpression?, isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver == explicitReceiver) {
            return explicitReceiverExpression
        }
        if (firReceiver is FirResolvedQualifier) {
            return convertToGetObject(firReceiver, callableReferenceMode = this is FirCallableReferenceAccess)
        }
        return firReceiver.takeIf { it !is FirNoReceiverExpression }?.let { visitor.convertToIrExpression(it) }
            ?: explicitReceiverExpression
            ?: run {
                if (this is FirCallableReferenceAccess) return null
                val name = if (isDispatch) "Dispatch" else "Extension"
                error("$name receiver expected: ${render()} to ${calleeReference.render()}")
            }
    }

    private fun FirQualifiedAccess.findBoundReceiver(explicitReceiverExpression: IrExpression?, isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver is FirNoReceiverExpression) {
            return null
        }
        return findIrReceiver(explicitReceiverExpression, isDispatch)
    }

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess, explicitReceiverExpression: IrExpression?): IrExpression {
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val ownerFunction =
                    symbol.owner as? IrFunction
                        ?: (symbol.owner as? IrProperty)?.getter
                if (ownerFunction?.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                if (ownerFunction?.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrFieldAccessExpression -> {
                val ownerField = symbol.owner
                if (!ownerField.isStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrBlockImpl -> apply {
                if (statements.isNotEmpty()) {
                    val lastStatement = statements.last()
                    if (lastStatement is IrExpression) {
                        statements[statements.size - 1] = lastStatement.applyReceivers(qualifiedAccess, explicitReceiverExpression)
                    }
                }
            }
            else -> this
        }
    }

    private fun generateErrorCallExpression(
        startOffset: Int,
        endOffset: Int,
        calleeReference: FirReference,
        type: IrType? = null
    ): IrErrorCallExpression {
        return IrErrorCallExpressionImpl(
            startOffset, endOffset, type ?: createErrorType(),
            "Unresolved reference: ${calleeReference.render()}"
        )
    }
}

