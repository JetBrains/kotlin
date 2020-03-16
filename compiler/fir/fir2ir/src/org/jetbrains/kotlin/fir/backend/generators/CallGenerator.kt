/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.defaultType

internal class CallGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    fun convertToIrCall(qualifiedAccess: FirQualifiedAccess, typeRef: FirTypeRef): IrExpression {
        val type = typeRef.toIrType()
        val symbol = qualifiedAccess.calleeReference.toSymbol(
            session,
            classifierStorage,
            declarationStorage
        )
        return typeRef.convertWithOffsets { startOffset, endOffset ->
            if (qualifiedAccess.calleeReference is FirSuperReference) {
                if (typeRef !is FirComposedSuperTypeRef) {
                    val dispatchReceiver = conversionScope.lastDispatchReceiverParameter()
                    if (dispatchReceiver != null) {
                        return@convertWithOffsets IrGetValueImpl(startOffset, endOffset, dispatchReceiver.type, dispatchReceiver.symbol)
                    }
                }
            }
            when {
                symbol is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                symbol is IrSimpleFunctionSymbol -> IrCallImpl(
                    startOffset, endOffset, type, symbol, origin = qualifiedAccess.calleeReference.statementOrigin()
                )
                symbol is IrPropertySymbol && symbol.isBound -> {
                    val getter = symbol.owner.getter
                    if (getter != null) {
                        IrCallImpl(startOffset, endOffset, type, getter.symbol, origin = IrStatementOrigin.GET_PROPERTY)
                    } else {
                        IrErrorCallExpressionImpl(
                            startOffset, endOffset, type, "No getter found for ${qualifiedAccess.calleeReference.render()}"
                        )
                    }
                }
                symbol is IrFieldSymbol -> IrGetFieldImpl(startOffset, endOffset, symbol, type, origin = IrStatementOrigin.GET_PROPERTY)
                symbol is IrValueSymbol -> IrGetValueImpl(
                    startOffset, endOffset, type, symbol,
                    origin = qualifiedAccess.calleeReference.statementOrigin()
                )
                symbol is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                else -> generateErrorCallExpression(startOffset, endOffset, qualifiedAccess.calleeReference, type)
            }
        }.applyCallArguments(qualifiedAccess as? FirCall).applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess)
    }

    fun convertToIrSetCall(variableAssignment: FirVariableAssignment): IrExpression {
        val calleeReference = variableAssignment.calleeReference
        val symbol = calleeReference.toSymbol(session, classifierStorage, declarationStorage)
        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            if (symbol != null && symbol.isBound) {
                when (symbol) {
                    is IrFieldSymbol -> IrSetFieldImpl(
                        startOffset, endOffset, symbol, typeConverter.unitType
                    ).apply {
                        value = visitor.convertToIrExpression(variableAssignment.rValue)
                    }
                    is IrPropertySymbol -> {
                        val irProperty = symbol.owner
                        val backingField = irProperty.backingField
                        if (backingField != null) {
                            IrSetFieldImpl(
                                startOffset, endOffset, backingField.symbol, typeConverter.unitType
                            ).apply {
                                value = visitor.convertToIrExpression(variableAssignment.rValue)
                            }
                        } else {
                            generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }
                    is IrVariableSymbol -> {
                        IrSetVariableImpl(
                            startOffset, endOffset, irBuiltIns.unitType, symbol,
                            visitor.convertToIrExpression(variableAssignment.rValue), null
                        )
                    }
                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                }
            } else {
                generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.applyReceivers(variableAssignment)
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
                        IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, type, irConstructor)
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
        val irType = qualifier.typeRef.toIrType()
        val typeRef = qualifier.typeRef as? FirResolvedTypeRef
        val classSymbol = (typeRef?.type as? ConeClassLikeType)?.lookupTag?.toSymbol(session)
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

    private fun IrExpression.applyCallArguments(call: FirCall?): IrExpression {
        if (call == null) return this
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        val argumentMapping = call.argumentMapping
                        if (argumentMapping != null && argumentMapping.isNotEmpty()) {
                            require(call is FirFunctionCall)
                            val function =
                                ((call.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
                            val valueParameters = function?.valueParameters
                            if (valueParameters != null) {
                                for ((argument, parameter) in argumentMapping) {
                                    val argumentExpression = visitor.convertToIrExpression(argument)
                                    putValueArgument(valueParameters.indexOf(parameter), argumentExpression)
                                }
                                return this
                            }
                        }
                        for ((index, argument) in call.arguments.withIndex()) {
                            val argumentExpression = visitor.convertToIrExpression(argument)
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

    private fun IrExpression.applyTypeArguments(access: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrCallWithIndexedArgumentsBase -> {
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
            else -> this
        }
    }

    private fun FirQualifiedAccess.findIrDispatchReceiver(): IrExpression? = findIrReceiver(isDispatch = true)

    private fun FirQualifiedAccess.findIrExtensionReceiver(): IrExpression? = findIrReceiver(isDispatch = false)

    private fun FirQualifiedAccess.findIrReceiver(isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver is FirResolvedQualifier) return convertToGetObject(firReceiver)
        return firReceiver.takeIf { it !is FirNoReceiverExpression }?.let { visitor.convertToIrExpression(it) }
            ?: explicitReceiver?.let { visitor.convertToIrExpression(it) } // NB: this applies to the situation when call is unresolved
            ?: run {
                // Object case
                val callableReference = calleeReference as? FirResolvedNamedReference
                val ownerClassId = (callableReference?.resolvedSymbol as? FirCallableSymbol<*>)?.callableId?.classId
                val ownerClassSymbol = ownerClassId?.let { session.firSymbolProvider.getClassLikeSymbolByFqName(it) }
                val firClass = (ownerClassSymbol?.fir as? FirClass)?.takeIf {
                    it is FirAnonymousObject || it is FirRegularClass && it.classKind == ClassKind.OBJECT
                }
                firClass?.convertWithOffsets { startOffset, endOffset ->
                    val irClass = classifierStorage.getCachedIrClass(firClass)!!
                    IrGetObjectValueImpl(startOffset, endOffset, irClass.defaultType, irClass.symbol)
                }
            }
        // TODO: uncomment after fixing KT-35730
//            ?: run {
//                val name = if (isDispatch) "Dispatch" else "Extension"
//                throw AssertionError(
//                    "$name receiver expected: ${render()} to ${calleeReference.render()}"
//                )
//            }
    }

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrCallImpl -> {
                val ownerFunction = symbol.owner
                if (ownerFunction.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver()
                }
                if (ownerFunction.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver()
                }
                this
            }
            is IrFieldExpressionBase -> {
                val ownerField = symbol.owner
                if (!ownerField.isStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver()
                }
                this
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