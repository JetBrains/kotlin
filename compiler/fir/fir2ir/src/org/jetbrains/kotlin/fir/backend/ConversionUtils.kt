/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.SyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(-1, -1)
    val startOffset = psi?.startOffsetSkippingComments ?: -1
    val endOffset = psi?.endOffset ?: -1
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

internal enum class ConversionTypeOrigin {
    DEFAULT,
    SETTER
}

class ConversionTypeContext internal constructor(
    internal val definitelyNotNull: Boolean,
    internal val origin: ConversionTypeOrigin
) {
    fun definitelyNotNull() = ConversionTypeContext(true, origin)

    fun inSetter() = ConversionTypeContext(definitelyNotNull, ConversionTypeOrigin.SETTER)

    companion object {
        internal val DEFAULT = ConversionTypeContext(
            definitelyNotNull = false, origin = ConversionTypeOrigin.DEFAULT
        )
    }
}

fun FirClassifierSymbol<*>.toSymbol(
    session: FirSession,
    classifierStorage: Fir2IrClassifierStorage,
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT
): IrClassifierSymbol {
    return when (this) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(this, typeContext)
        }
        is FirTypeAliasSymbol -> {
            val typeAlias = fir
            val coneClassLikeType = (typeAlias.expandedTypeRef as FirResolvedTypeRef).type as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)!!.toSymbol(session, classifierStorage)
        }
        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(this)
        }
        else -> throw AssertionError("Should not be here: $this")
    }
}

fun FirReference.toSymbol(
    session: FirSession,
    classifierStorage: Fir2IrClassifierStorage,
    declarationStorage: Fir2IrDeclarationStorage,
    conversionScope: Fir2IrConversionScope,
    preferGetter: Boolean = true
): IrSymbol? {
    return when (this) {
        is FirResolvedNamedReference -> {
            when (val resolvedSymbol = resolvedSymbol) {
                is FirCallableSymbol<*> -> {
                    resolvedSymbol.deepestMatchingOverriddenSymbol().toSymbol(declarationStorage, preferGetter)
                }
                is FirClassifierSymbol<*> -> {
                    resolvedSymbol.toSymbol(session, classifierStorage)
                }
                else -> {
                    throw AssertionError("Unknown symbol: $resolvedSymbol")
                }
            }
        }
        is FirThisReference -> {
            when (val boundSymbol = boundSymbol) {
                is FirClassSymbol<*> -> classifierStorage.getIrClassSymbol(boundSymbol).owner.thisReceiver?.symbol
                is FirFunctionSymbol -> declarationStorage.getIrFunctionSymbol(boundSymbol).owner.extensionReceiverParameter?.symbol
                is FirPropertySymbol -> {
                    val property = declarationStorage.getIrPropertyOrFieldSymbol(boundSymbol).owner as? IrProperty
                    property?.let { conversionScope.parentAccessorOfPropertyFromStack(it) }?.symbol
                }
                else -> null
            }
        }
        else -> null
    }
}

private fun FirCallableSymbol<*>.toSymbol(declarationStorage: Fir2IrDeclarationStorage, preferGetter: Boolean): IrSymbol? = when (this) {
    is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(this)
    is SyntheticPropertySymbol -> {
        (fir as? FirSyntheticProperty)?.let { syntheticProperty ->
            if (preferGetter) {
                syntheticProperty.getter.delegate.symbol.toSymbol(declarationStorage, preferGetter)
            } else {
                syntheticProperty.setter!!.delegate.symbol.toSymbol(declarationStorage, preferGetter)
            }
        } ?: if (fir.isLocal) declarationStorage.getIrValueSymbol(this) else declarationStorage.getIrPropertyOrFieldSymbol(this)
    }
    is FirPropertySymbol -> {
        if (fir.isLocal) declarationStorage.getIrValueSymbol(this) else declarationStorage.getIrPropertyOrFieldSymbol(this)
    }
    is FirFieldSymbol -> declarationStorage.getIrPropertyOrFieldSymbol(this)
    is FirBackingFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(this)
    is FirDelegateFieldSymbol<*> -> declarationStorage.getIrBackingFieldSymbol(this)
    is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(this)
    else -> null
}

fun FirConstExpression<*>.getIrConstKind(): IrConstKind<*> = when (kind) {
    FirConstKind.IntegerLiteral -> {
        val type = typeRef.coneTypeUnsafe<ConeIntegerLiteralType>()
        type.getApproximatedType().toConstKind()!!.toIrConstKind()
    }
    else -> kind.toIrConstKind()
}

fun <T> FirConstExpression<T>.toIrConst(irType: IrType): IrConst<T> {
    return convertWithOffsets { startOffset, endOffset ->
        @Suppress("UNCHECKED_CAST")
        val kind = getIrConstKind() as IrConstKind<T>

        @Suppress("UNCHECKED_CAST")
        val value = (value as? Long)?.let {
            when (kind) {
                IrConstKind.Byte -> it.toByte()
                IrConstKind.Short -> it.toShort()
                IrConstKind.Int -> it.toInt()
                IrConstKind.Float -> it.toFloat()
                IrConstKind.Double -> it.toDouble()
                else -> it
            }
        } as T ?: value
        IrConstImpl(
            startOffset, endOffset,
            irType,
            kind, value
        )
    }
}

private fun FirConstKind<*>.toIrConstKind(): IrConstKind<*> = when (this) {
    FirConstKind.Null -> IrConstKind.Null
    FirConstKind.Boolean -> IrConstKind.Boolean
    FirConstKind.Char -> IrConstKind.Char

    FirConstKind.Byte -> IrConstKind.Byte
    FirConstKind.Short -> IrConstKind.Short
    FirConstKind.Int -> IrConstKind.Int
    FirConstKind.Long -> IrConstKind.Long

    FirConstKind.UnsignedByte -> IrConstKind.Byte
    FirConstKind.UnsignedShort -> IrConstKind.Short
    FirConstKind.UnsignedInt -> IrConstKind.Int
    FirConstKind.UnsignedLong -> IrConstKind.Long

    FirConstKind.String -> IrConstKind.String
    FirConstKind.Float -> IrConstKind.Float
    FirConstKind.Double -> IrConstKind.Double
    FirConstKind.IntegerLiteral, FirConstKind.UnsignedIntegerLiteral -> throw IllegalArgumentException()
}

internal fun FirClass<*>.collectCallableNamesFromSupertypes(session: FirSession, result: MutableList<Name> = mutableListOf()): List<Name> {
    for (superTypeRef in superTypeRefs) {
        superTypeRef.collectCallableNamesFromThisAndSupertypes(session, result)
    }
    return result
}

private fun FirTypeRef.collectCallableNamesFromThisAndSupertypes(
    session: FirSession,
    result: MutableList<Name> = mutableListOf()
): List<Name> {
    if (this is FirResolvedTypeRef) {
        val superType = type
        if (superType is ConeClassLikeType) {
            when (val superSymbol = superType.lookupTag.toSymbol(session)) {
                is FirClassSymbol -> {
                    val superClass = superSymbol.fir as FirClass<*>
                    for (declaration in superClass.declarations) {
                        when (declaration) {
                            is FirSimpleFunction -> result += declaration.name
                            is FirVariable<*> -> result += declaration.name
                        }
                    }
                    superClass.collectCallableNamesFromSupertypes(session, result)
                }
                is FirTypeAliasSymbol -> {
                    val superAlias = superSymbol.fir
                    superAlias.expandedTypeRef.collectCallableNamesFromThisAndSupertypes(session, result)
                }
            }
        }
    }
    return result
}

internal tailrec fun FirCallableSymbol<*>.deepestOverriddenSymbol(): FirCallableSymbol<*> {
    val overriddenSymbol = overriddenSymbol ?: return this
    return overriddenSymbol.deepestOverriddenSymbol()
}

internal tailrec fun FirCallableSymbol<*>.deepestMatchingOverriddenSymbol(root: FirCallableSymbol<*> = this): FirCallableSymbol<*> {
    val overriddenSymbol = overriddenSymbol?.takeIf { it.callableId == root.callableId } ?: return this
    return overriddenSymbol.deepestMatchingOverriddenSymbol(this)
}

private val nameToOperationConventionOrigin = mutableMapOf(
    OperatorNameConventions.PLUS to IrStatementOrigin.PLUS,
    OperatorNameConventions.MINUS to IrStatementOrigin.MINUS,
    OperatorNameConventions.TIMES to IrStatementOrigin.MUL,
    OperatorNameConventions.DIV to IrStatementOrigin.DIV,
    OperatorNameConventions.MOD to IrStatementOrigin.PERC,
    OperatorNameConventions.REM to IrStatementOrigin.PERC,
    OperatorNameConventions.RANGE_TO to IrStatementOrigin.RANGE
)

internal fun FirReference.statementOrigin(): IrStatementOrigin? {
    return when (this) {
        is FirPropertyFromParameterResolvedNamedReference -> IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
        is FirResolvedNamedReference -> when (resolvedSymbol) {
            is AccessorSymbol, is SyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
            is FirNamedFunctionSymbol -> when {
                resolvedSymbol.callableId.isInvoke() ->
                    IrStatementOrigin.INVOKE
                source?.elementType == KtNodeTypes.FOR && resolvedSymbol.callableId.isIteratorNext() ->
                    IrStatementOrigin.FOR_LOOP_NEXT
                source?.elementType == KtNodeTypes.FOR && resolvedSymbol.callableId.isIteratorHasNext() ->
                    IrStatementOrigin.FOR_LOOP_HAS_NEXT
                source?.elementType == KtNodeTypes.FOR && resolvedSymbol.callableId.isIterator() ->
                    IrStatementOrigin.FOR_LOOP_ITERATOR
                source?.elementType == KtNodeTypes.OPERATION_REFERENCE ->
                    nameToOperationConventionOrigin[resolvedSymbol.callableId.callableName]
                else ->
                    null
            }
            else -> null
        }
        else -> null
    }
}

fun FirClass<*>.getPrimaryConstructorIfAny(): FirConstructor? =
    declarations.filterIsInstance<FirConstructor>().firstOrNull()?.takeIf { it.isPrimary }

internal fun IrDeclarationParent.declareThisReceiverParameter(
    symbolTable: SymbolTable,
    thisType: IrType,
    thisOrigin: IrDeclarationOrigin,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset
): IrValueParameter {
    val receiverDescriptor = WrappedReceiverParameterDescriptor()
    return symbolTable.declareValueParameter(
        startOffset, endOffset, thisOrigin, receiverDescriptor, thisType
    ) { symbol ->
        IrValueParameterImpl(
            startOffset, endOffset, thisOrigin, symbol,
            Name.special("<this>"), -1, thisType,
            varargElementType = null, isCrossinline = false, isNoinline = false
        ).apply {
            this.parent = this@declareThisReceiverParameter
            receiverDescriptor.bind(this)
        }
    }
}

