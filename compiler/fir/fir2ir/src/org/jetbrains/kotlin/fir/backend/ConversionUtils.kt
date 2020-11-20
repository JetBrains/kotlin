/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.SyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.scopes.processDirectlyOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processDirectlyOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    val startOffset = psi?.startOffsetSkippingComments ?: UNDEFINED_OFFSET
    val endOffset = psi?.endOffset ?: UNDEFINED_OFFSET
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
            val coneClassLikeType = typeAlias.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)!!.toSymbol(session, classifierStorage)
        }
        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(this)
        }
        else -> error("Unknown symbol: $this")
    }
}

fun FirReference.toSymbolForCall(
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
                    resolvedSymbol.unwrapCallRepresentative().toSymbolForCall(declarationStorage, preferGetter)
                }
                is FirClassifierSymbol<*> -> {
                    resolvedSymbol.toSymbol(session, classifierStorage)
                }
                else -> {
                    error("Unknown symbol: $resolvedSymbol")
                }
            }
        }
        is FirThisReference -> {
            when (val boundSymbol = boundSymbol) {
                is FirClassSymbol<*> -> classifierStorage.getIrClassSymbol(boundSymbol).owner.thisReceiver?.symbol
                is FirFunctionSymbol -> declarationStorage.getIrFunctionSymbol(boundSymbol).owner.extensionReceiverParameter?.symbol
                is FirPropertySymbol -> {
                    val property = declarationStorage.getIrPropertySymbol(boundSymbol).owner as? IrProperty
                    property?.let { conversionScope.parentAccessorOfPropertyFromStack(it) }?.symbol
                }
                else -> null
            }
        }
        else -> null
    }
}

private fun FirCallableSymbol<*>.toSymbolForCall(declarationStorage: Fir2IrDeclarationStorage, preferGetter: Boolean): IrSymbol? = when (this) {
    is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(this)
    is SyntheticPropertySymbol -> {
        (fir as? FirSyntheticProperty)?.let { syntheticProperty ->
            val delegateSymbol = if (preferGetter) {
                syntheticProperty.getter.delegate.symbol
            } else {
                syntheticProperty.setter?.delegate?.symbol
                    ?: throw AssertionError("Written synthetic property must have a setter")
            }
            delegateSymbol.unwrapCallRepresentative().toSymbolForCall(declarationStorage, preferGetter)
        } ?: declarationStorage.getIrPropertySymbol(this)
    }
    is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(this)
    is FirFieldSymbol -> declarationStorage.getIrFieldSymbol(this)
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


internal tailrec fun FirCallableSymbol<*>.unwrapSubstitutionAndIntersectionOverrides(): FirCallableSymbol<*> {
    val originalForSubstitutionOverride = originalForSubstitutionOverride
    if (originalForSubstitutionOverride != null) return originalForSubstitutionOverride.unwrapSubstitutionAndIntersectionOverrides()

    val baseForIntersectionOverride = baseForIntersectionOverride
    if (baseForIntersectionOverride != null) return baseForIntersectionOverride.unwrapSubstitutionAndIntersectionOverrides()

    return this
}

internal tailrec fun FirCallableSymbol<*>.unwrapCallRepresentative(root: FirCallableSymbol<*> = this): FirCallableSymbol<*> {
    val fir = fir
    if (fir is FirConstructor) {
        val originalForTypeAlias = fir.originalConstructorIfTypeAlias
        if (originalForTypeAlias != null) return originalForTypeAlias.symbol.unwrapCallRepresentative(this)
    }
    if (fir.isIntersectionOverride) return this

    val overriddenSymbol = fir.originalForSubstitutionOverride?.takeIf {
        it.containingClass() == root.containingClass()
    }?.symbol ?: return this

    return overriddenSymbol.unwrapCallRepresentative(this)
}

internal fun FirSimpleFunction.generateOverriddenFunctionSymbols(
    containingClass: FirClass<*>,
    session: FirSession,
    scopeSession: ScopeSession,
    declarationStorage: Fir2IrDeclarationStorage
): List<IrSimpleFunctionSymbol> {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
    scope.processFunctionsByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    scope.processDirectlyOverriddenFunctions(symbol) {
        if ((it.fir as FirSimpleFunction).visibility == Visibilities.Private) {
            return@processDirectlyOverriddenFunctions ProcessorAction.NEXT
        }

        val overridden = declarationStorage.getIrFunctionSymbol(it.unwrapFakeOverrides())
        overriddenSet += overridden as IrSimpleFunctionSymbol
        ProcessorAction.NEXT
    }
    return overriddenSet.toList()
}

internal fun FirProperty.generateOverriddenAccessorSymbols(
    containingClass: FirClass<*>,
    isGetter: Boolean,
    session: FirSession,
    scopeSession: ScopeSession,
    declarationStorage: Fir2IrDeclarationStorage
): List<IrSimpleFunctionSymbol> {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    scope.processDirectlyOverriddenProperties(symbol) {
        if (it is FirAccessorSymbol || it.fir.visibility == Visibilities.Private) {
            return@processDirectlyOverriddenProperties ProcessorAction.NEXT
        }

        val unwrapped =
            it.fir.delegatedWrapperData?.takeIf { it.containingClass == containingClass.symbol.toLookupTag() }?.wrapped?.symbol ?: it

        val overriddenProperty = declarationStorage.getIrPropertySymbol(unwrapped.unwrapFakeOverrides()) as IrPropertySymbol
        val overriddenAccessor = if (isGetter) overriddenProperty.owner.getter?.symbol else overriddenProperty.owner.setter?.symbol
        if (overriddenAccessor != null) {
            overriddenSet += overriddenAccessor
        }
        ProcessorAction.NEXT
    }
    return overriddenSet.toList()
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
        is FirResolvedNamedReference -> when (val symbol = resolvedSymbol) {
            is AccessorSymbol, is SyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
            is FirNamedFunctionSymbol -> when {
                symbol.callableId.isInvoke() ->
                    IrStatementOrigin.INVOKE
                source?.elementType == KtNodeTypes.FOR && symbol.callableId.isIteratorNext() ->
                    IrStatementOrigin.FOR_LOOP_NEXT
                source?.elementType == KtNodeTypes.FOR && symbol.callableId.isIteratorHasNext() ->
                    IrStatementOrigin.FOR_LOOP_HAS_NEXT
                source?.elementType == KtNodeTypes.FOR && symbol.callableId.isIterator() ->
                    IrStatementOrigin.FOR_LOOP_ITERATOR
                source?.elementType == KtNodeTypes.OPERATION_REFERENCE ->
                    nameToOperationConventionOrigin[symbol.callableId.callableName]
                else ->
                    null
            }
            else -> null
        }
        else -> null
    }
}

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
        symbolTable.irFactory.createValueParameter(
            startOffset, endOffset, thisOrigin, symbol,
            Name.special("<this>"), UNDEFINED_PARAMETER_INDEX, thisType,
            varargElementType = null, isCrossinline = false, isNoinline = false, isAssignable = false
        ).apply {
            this.parent = this@declareThisReceiverParameter
            receiverDescriptor.bind(this)
        }
    }
}

fun FirClass<*>.irOrigin(firProvider: FirProvider): IrDeclarationOrigin = when {
    firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    origin == FirDeclarationOrigin.Java -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
}

val IrType.isSamType: Boolean
    get() {
        val irClass = classOrNull ?: return false
        if (irClass.owner.kind != ClassKind.INTERFACE) return false
        val am = irClass.functions.singleOrNull { it.owner.modality == Modality.ABSTRACT }
        return am != null
    }

fun Fir2IrComponents.createSafeCallConstruction(
    receiverVariable: IrVariable,
    receiverVariableSymbol: IrValueSymbol,
    expressionOnNotNull: IrExpression,
): IrExpression {
    val startOffset = expressionOnNotNull.startOffset
    val endOffset = expressionOnNotNull.endOffset

    val resultType = expressionOnNotNull.type.makeNullable()
    return IrBlockImpl(startOffset, endOffset, resultType, IrStatementOrigin.SAFE_CALL).apply {
        statements += receiverVariable
        statements += IrWhenImpl(startOffset, endOffset, resultType).apply {
            val condition = IrCallImpl(
                startOffset, endOffset, irBuiltIns.booleanType,
                irBuiltIns.eqeqSymbol,
                valueArgumentsCount = 2,
                typeArgumentsCount = 0,
                origin = IrStatementOrigin.EQEQ
            ).apply {
                putValueArgument(0, IrGetValueImpl(startOffset, endOffset, receiverVariableSymbol))
                putValueArgument(1, IrConstImpl.constNull(startOffset, endOffset, irBuiltIns.nothingNType))
            }
            branches += IrBranchImpl(
                condition, IrConstImpl.constNull(startOffset, endOffset, irBuiltIns.nothingNType)
            )
            branches += IrElseBranchImpl(
                IrConstImpl.boolean(startOffset, endOffset, irBuiltIns.booleanType, true),
                expressionOnNotNull
            )
        }
    }
}

fun Fir2IrComponents.createTemporaryVariable(
    receiverExpression: IrExpression,
    conversionScope: Fir2IrConversionScope,
    nameHint: String? = null
): Pair<IrVariable, IrValueSymbol> {
    val receiverVariable = declarationStorage.declareTemporaryVariable(receiverExpression, nameHint).apply {
        parent = conversionScope.parentFromStack()
    }
    val variableSymbol = receiverVariable.symbol

    return Pair(receiverVariable, variableSymbol)
}

fun Fir2IrComponents.createTemporaryVariableForSafeCallConstruction(
    receiverExpression: IrExpression,
    conversionScope: Fir2IrConversionScope
): Pair<IrVariable, IrValueSymbol> =
    createTemporaryVariable(receiverExpression, conversionScope, "safe_receiver")
