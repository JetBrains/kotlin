/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
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
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi2ir.generators.hasNoSideEffects

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
                    val function = symbol.owner
                    IrFunctionReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = function.typeParameters.size,
                        valueArgumentsCount = function.valueParameters.size,
                        reflectionTarget = symbol
                    )
                }
                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
                    )
                }
            }
        }.applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
    }

    private fun FirQualifiedAccess.tryConvertToSamConstructorCall(type: IrType): IrTypeOperatorCall? {
        val calleeReference = calleeReference as? FirResolvedNamedReference ?: return null
        val fir = calleeReference.resolvedSymbol.fir
        if (this is FirFunctionCall && fir is FirSimpleFunction && fir.origin == FirDeclarationOrigin.SamConstructor) {
            return convertWithOffsets { startOffset, endOffset ->
                IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.SAM_CONVERSION, type).apply {
                    argument = visitor.convertToIrExpression(this@tryConvertToSamConstructorCall.argument)
                }
            }
        }
        return null
    }

    fun convertToIrCall(
        qualifiedAccess: FirQualifiedAccess,
        typeRef: FirTypeRef,
        explicitReceiverExpression: IrExpression?
    ): IrExpression {
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
            var superQualifierSymbol: IrClassSymbol? = null
            if (dispatchReceiver is FirQualifiedAccess) {
                val dispatchReceiverReference = dispatchReceiver.calleeReference
                if (dispatchReceiverReference is FirSuperReference) {
                    val superTypeRef = dispatchReceiverReference.superTypeRef
                    val coneSuperType = superTypeRef.coneTypeSafe<ConeClassLikeType>()
                    if (coneSuperType != null) {
                        val firClassSymbol = coneSuperType.lookupTag.toSymbol(session) as? FirClassSymbol<*>
                        if (firClassSymbol != null) {
                            superQualifierSymbol = classifierStorage.getIrClassSymbol(firClassSymbol)
                        }
                    } else if (superTypeRef is FirComposedSuperTypeRef) {
                        val owner = symbol?.owner
                        if (owner != null && owner is IrDeclaration) {
                            superQualifierSymbol = owner.parentClassOrNull?.symbol
                        }
                    }
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
                        superQualifierSymbol = superQualifierSymbol
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
                            superQualifierSymbol = superQualifierSymbol
                        )
                        backingField != null -> IrGetFieldImpl(
                            startOffset, endOffset, backingField.symbol, type,
                            superQualifierSymbol = superQualifierSymbol
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
                    superQualifierSymbol = superQualifierSymbol
                )
                is IrValueSymbol -> IrGetValueImpl(
                    startOffset, endOffset, type, symbol,
                    origin = qualifiedAccess.calleeReference.statementOrigin()
                )
                is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                else -> generateErrorCallExpression(startOffset, endOffset, qualifiedAccess.calleeReference, type)
            }
        }.applyCallArguments(qualifiedAccess as? FirCall)
            .applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, explicitReceiverExpression)
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
                is IrPropertySymbol -> {
                    val irProperty = symbol.owner
                    val setter = irProperty.setter
                    val backingField = irProperty.backingField
                    when {
                        setter != null -> IrCallImpl(
                            startOffset, endOffset, type, setter.symbol,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                        backingField != null -> IrSetFieldImpl(startOffset, endOffset, backingField.symbol, type).apply {
                            // NB: to be consistent with FIR2IR, origin should be null here
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
                is IrVariableSymbol -> IrSetVariableImpl(startOffset, endOffset, type, symbol, assignedValue, origin)
                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.applyTypeArguments(variableAssignment).applyReceivers(variableAssignment, explicitReceiverExpression)
    }

    fun convertToIrConstructorCall(annotationCall: FirAnnotationCall): IrExpression {
        val coneType = (annotationCall.annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeLookupTagBasedType
        val firSymbol = coneType?.lookupTag?.toSymbol(session) as? FirClassSymbol
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        return annotationCall.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val fir = firSymbol?.fir as? FirClass<*>
                    val irConstructor = fir?.getPrimaryConstructorIfAny()?.let { firConstructor ->
                        this.declarationStorage.getIrConstructorSymbol(firConstructor.symbol)
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
        }.applyCallArguments(annotationCall)
    }

    fun convertToGetObject(qualifier: FirResolvedQualifier): IrExpression {
        return convertToGetObject(qualifier, callableReferenceMode = false)!!
    }

    internal fun convertToGetObject(qualifier: FirResolvedQualifier, callableReferenceMode: Boolean): IrExpression? {
        val typeRef = qualifier.typeRef as? FirResolvedTypeRef
        val classSymbol = (typeRef?.type as? ConeClassLikeType)?.lookupTag?.toSymbol(session)
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

    internal fun IrExpression.applyCallArguments(call: FirCall?): IrExpression {
        if (call == null) return this
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        val calleeReference = when (call) {
                            is FirFunctionCall -> call.calleeReference
                            is FirDelegatedConstructorCall -> call.calleeReference
                            else -> null
                        } as? FirResolvedNamedReference
                        val function = (calleeReference?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
                        val valueParameters = function?.valueParameters
                        val argumentMapping = call.argumentMapping
                        if (argumentMapping != null && argumentMapping.isNotEmpty()) {
                            if (valueParameters != null) {
                                return applyArgumentsWithReorderingIfNeeded(argumentMapping, valueParameters)
                            }
                        }
                        for ((index, argument) in call.arguments.withIndex()) {
                            val argumentExpression =
                                visitor.convertToIrExpression(argument).applySamConversionIfNeeded(argument, valueParameters?.get(index))
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

    private fun IrCallWithIndexedArgumentsBase.applyArgumentsWithReorderingIfNeeded(
        argumentMapping: Map<FirExpression, FirValueParameter>,
        valueParameters: List<FirValueParameter>,
    ): IrExpressionBase {
        if (needArgumentReordering(argumentMapping.values, valueParameters)) {
            return IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL).apply {
                for ((argument, parameter) in argumentMapping) {
                    val parameterIndex = valueParameters.indexOf(parameter)
                    val irArgument = visitor.convertToIrExpression(argument).applySamConversionIfNeeded(argument, parameter)
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
                val argumentExpression = visitor.convertToIrExpression(argument).applySamConversionIfNeeded(argument, parameter)
                putValueArgument(valueParameters.indexOf(parameter), argumentExpression)
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
        if (parameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true) {
            return false
        }
        // On the other hand, the actual type should be a functional type.
        return argument.isFunctional(session)
    }

    private fun IrExpression.applyTypeArguments(access: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrMemberAccessExpressionBase -> {
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
            // TODO: remove after fix of KT-35730 (temporary hack to prevent receiver duplication)
            if (!isDispatch && dispatchReceiver is FirNoReceiverExpression) {
                return visitor.convertToIrExpression(firReceiver)
            }
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
                throw AssertionError(
                    "$name receiver expected: ${render()} to ${calleeReference.render()}"
                )
            }
    }

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess, explicitReceiverExpression: IrExpression?): IrExpression {
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val ownerFunction = symbol.owner as? IrFunction
                if (ownerFunction?.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                if (ownerFunction?.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrNoArgumentsCallableReferenceBase -> {
                val ownerPropertyGetter = (symbol.owner as? IrProperty)?.getter
                if (ownerPropertyGetter?.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                if (ownerPropertyGetter?.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrFieldExpressionBase -> {
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

