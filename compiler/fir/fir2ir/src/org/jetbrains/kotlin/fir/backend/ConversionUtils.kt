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
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
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
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.Fir2IrClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
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

private val simpleDeclarationCollector: (FirDeclaration, MutableMap<Name, FirDeclaration>) -> Unit = { declaration, map ->
    when (declaration) {
        is FirSimpleFunction ->
            map.putIfAbsent(declaration.name, declaration)
        is FirVariable<*> ->
            map.putIfAbsent(declaration.name, declaration)
    }
}

internal fun FirClass<*>.collectCallableNamesFromSupertypes(session: FirSession): Set<Name> {
    val result = mutableMapOf<Name, FirDeclaration>()
    for (superTypeRef in superTypeRefs) {
        superTypeRef.collectDeclarationsFromThisAndSupertypes(session, result, simpleDeclarationCollector)
    }
    return result.keys
}

internal fun FirClass<*>.collectContributedFunctionsFromSupertypes(
    session: FirSession,
    record: (FirDeclaration, MutableMap<Name, FirDeclaration>) -> Unit
): Map<Name, FirDeclaration> {
    val result = mutableMapOf<Name, FirDeclaration>()
    for (superTypeRef in superTypeRefs) {
        superTypeRef.collectDeclarationsFromThisAndSupertypes(session, result, record)
    }
    return result
}

private fun FirClass<*>.collectDeclarationsFromSupertypes(
    session: FirSession,
    result: MutableMap<Name, FirDeclaration>,
    record: (FirDeclaration, MutableMap<Name, FirDeclaration>) -> Unit
): Map<Name, FirDeclaration> {
    for (superTypeRef in superTypeRefs) {
        superTypeRef.collectDeclarationsFromThisAndSupertypes(session, result, record)
    }
    return result
}

private fun FirTypeRef.collectDeclarationsFromThisAndSupertypes(
    session: FirSession,
    result: MutableMap<Name, FirDeclaration>,
    record: (FirDeclaration, MutableMap<Name, FirDeclaration>) -> Unit
): Map<Name, FirDeclaration> {
    if (this is FirResolvedTypeRef) {
        val superType = type
        if (superType is ConeClassLikeType) {
            when (val superSymbol = superType.lookupTag.toSymbol(session)) {
                is FirClassSymbol -> {
                    val superClass = superSymbol.fir as FirClass<*>
                    for (declaration in superClass.declarations) {
                        record(declaration, result)
                    }
                    superClass.collectDeclarationsFromSupertypes(session, result, record)
                }
                is FirTypeAliasSymbol -> {
                    val superAlias = superSymbol.fir
                    superAlias.expandedTypeRef.collectDeclarationsFromThisAndSupertypes(session, result, record)
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

internal fun IrClass.findMatchingOverriddenSymbolsFromSupertypes(
    irBuiltIns: IrBuiltIns,
    target: IrDeclaration,
    result: MutableList<IrSymbol> = mutableListOf(),
    visited: MutableSet<IrClass> = mutableSetOf()
): List<IrSymbol> {
    for (superType in superTypes) {
        when (val superTypeClass = superType.classOrNull) {
            is IrClassSymbolImpl, is IrClassPublicSymbolImpl, is Fir2IrClassSymbol -> {
                superTypeClass.owner.findMatchingOverriddenSymbolsFromThisAndSupertypes(irBuiltIns, target, result, visited)
            }
        }
    }
    return result
}

private fun IrClass.findMatchingOverriddenSymbolsFromThisAndSupertypes(
    irBuiltIns: IrBuiltIns,
    target: IrDeclaration,
    result: MutableList<IrSymbol>,
    visited: MutableSet<IrClass>
): List<IrSymbol> {
    if (this in visited) {
        return result
    }
    visited += this
    val targetIsPropertyAccessor = target is IrFunction && target.isPropertyAccessor
    for (declaration in declarations) {
        if (declaration.isFakeOverride || declaration is IrConstructor) {
            continue
        }
        when {
            declaration is IrSimpleFunction && target is IrSimpleFunction ->
                if (declaration.modality != Modality.FINAL &&
                    !Visibilities.isPrivate(declaration.visibility) &&
                    isOverriding(irBuiltIns, target, declaration)
                ) {
                    result.add(declaration.symbol)
                }
            declaration is IrProperty && (target is IrField || targetIsPropertyAccessor) -> {
                val backingField = declaration.backingField
                if (target is IrField && backingField != null) {
                    if (!backingField.isFinal && !backingField.isStatic &&
                        !Visibilities.isPrivate(backingField.visibility) &&
                        isOverriding(irBuiltIns, target, backingField)
                    ) {
                        result.add(backingField.symbol)
                    }
                }
                if (targetIsPropertyAccessor) {
                    val getter = declaration.getter
                    if (getter != null) {
                        if (getter.modality != Modality.FINAL &&
                            !Visibilities.isPrivate(getter.visibility) &&
                            isOverriding(irBuiltIns, target, getter)
                        ) {
                            result.add(getter.symbol)
                        }
                    }
                    val setter = declaration.setter
                    if (setter != null) {
                        if (setter.modality != Modality.FINAL &&
                            !Visibilities.isPrivate(setter.visibility) &&
                            isOverriding(irBuiltIns, target, setter)
                        ) {
                            result.add(setter.symbol)
                        }
                    }
                }
            }
        }
    }
    // Stop traversing upwards if we find matching overridden symbols at this level.
    if (result.isNotEmpty()) {
        return result
    }
    return findMatchingOverriddenSymbolsFromSupertypes(irBuiltIns, target, result, visited)
}

fun isOverriding(
    irBuiltIns: IrBuiltIns,
    target: IrDeclaration,
    superCandidate: IrDeclaration
): Boolean {
    val typeCheckerContext = IrTypeCheckerContext(irBuiltIns) as AbstractTypeCheckerContext
    fun equalTypes(first: IrType, second: IrType): Boolean {
        if (first is IrErrorType || second is IrErrorType) return false
        return AbstractTypeChecker.equalTypes(
            typeCheckerContext, first, second
        ) ||
                // TODO: should pass type parameter cache, and make sure target type is indeed a matched type argument.
                second.classifierOrNull is IrTypeParameterSymbol

    }

    return when {
        target is IrFunction && superCandidate is IrFunction -> {
            // Not checking the return type (they should match each other if everything other match, otherwise it's a compilation error)
            target.name == superCandidate.name &&
                    target.extensionReceiverParameter?.type?.let {
                        val superCandidateReceiverType = superCandidate.extensionReceiverParameter?.type
                        superCandidateReceiverType != null && equalTypes(it, superCandidateReceiverType)
                    } != false &&
                    target.valueParameters.size == superCandidate.valueParameters.size &&
                    target.valueParameters.zip(superCandidate.valueParameters).all { (targetParameter, superCandidateParameter) ->
                        equalTypes(targetParameter.type, superCandidateParameter.type)
                    }
        }
        target is IrField && superCandidate is IrField -> {
            // Not checking the field type (they should match each other if everything other match, otherwise it's a compilation error)
            target.name == superCandidate.name
        }
        else -> false
    }
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

fun FirClass<*>.irOrigin(firProvider: FirProvider): IrDeclarationOrigin = when {
    firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    origin == FirDeclarationOrigin.Java -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
}

fun FirClass<*>.getSamIfAny(): FirSimpleFunction? =
    declarations.filterIsInstance<FirSimpleFunction>()
        .singleOrNull { it.modality == Modality.ABSTRACT && !it.isPublicInObject(checkOnlyName = true) }

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
    isReceiverNullable: Boolean
): IrExpression {
    val startOffset = expressionOnNotNull.startOffset
    val endOffset = expressionOnNotNull.endOffset

    val resultType = expressionOnNotNull.type.let { if (isReceiverNullable) it.makeNullable() else it }
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

fun Fir2IrComponents.createTemporaryVariableForSafeCallConstruction(
    receiverExpression: IrExpression,
    conversionScope: Fir2IrConversionScope
): Pair<IrVariable, IrValueSymbol> {
    val receiverVariable = declarationStorage.declareTemporaryVariable(receiverExpression, "safe_receiver").apply {
        parent = conversionScope.parentFromStack()
    }
    val variableSymbol = receiverVariable.symbol

    return Pair(receiverVariable, variableSymbol)
}
