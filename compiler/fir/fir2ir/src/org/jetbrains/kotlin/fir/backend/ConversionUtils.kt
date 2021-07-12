/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.generators.FakeOverrideGenerator
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val psi = psi
    if (psi is PsiCompiledElement || psi == null) return f(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    val startOffset = psi.startOffsetSkippingComments
    val endOffset = psi.endOffset
    return f(startOffset, endOffset)
}

internal fun <T : IrElement> FirQualifiedAccess.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val psi = psi
    if (psi is PsiCompiledElement || psi == null) return f(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    val startOffset = if (psi is KtQualifiedExpression) {
        (psi.selectorExpression ?: psi).startOffsetSkippingComments
    } else {
        psi.startOffsetSkippingComments
    }
    val endOffset = psi.endOffset
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

internal enum class ConversionTypeOrigin {
    DEFAULT,
    SETTER
}

class ConversionTypeContext internal constructor(
    internal val definitelyNotNull: Boolean,
    internal val invariantProjection: Boolean = false,
    internal val origin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
) {
    fun definitelyNotNull() = ConversionTypeContext(
        definitelyNotNull = true,
        invariantProjection = invariantProjection,
        origin = origin
    )

    fun inSetter() = ConversionTypeContext(
        definitelyNotNull = definitelyNotNull,
        invariantProjection = invariantProjection,
        origin = ConversionTypeOrigin.SETTER
    )

    fun withInvariantProjections() = ConversionTypeContext(
        definitelyNotNull = definitelyNotNull,
        invariantProjection = true,
        origin = origin
    )

    companion object {
        internal val DEFAULT = ConversionTypeContext(
            definitelyNotNull = false, origin = ConversionTypeOrigin.DEFAULT, invariantProjection = false
        )
        internal val WITH_INVARIANT = DEFAULT.withInvariantProjections()
    }
}

fun FirClassifierSymbol<*>.toSymbol(
    session: FirSession,
    classifierStorage: Fir2IrClassifierStorage,
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
    handleAnnotations: ((List<FirAnnotationCall>) -> Unit)? = null
): IrClassifierSymbol {
    return when (this) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(this, typeContext)
        }
        is FirTypeAliasSymbol -> {
            handleAnnotations?.invoke(fir.expandedTypeRef.annotations)
            val coneClassLikeType = fir.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)!!.toSymbol(session, classifierStorage, typeContext, handleAnnotations)
        }
        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(this)
        }
        else -> error("Unknown symbol: $this")
    }
}

private fun FirBasedSymbol<*>.toSymbolForCall(
    session: FirSession,
    classifierStorage: Fir2IrClassifierStorage,
    declarationStorage: Fir2IrDeclarationStorage,
    preferGetter: Boolean
) = when (this) {
    is FirCallableSymbol<*> -> unwrapCallRepresentative().toSymbolForCall(declarationStorage, preferGetter)
    is FirClassifierSymbol<*> -> toSymbol(session, classifierStorage)
    else -> error("Unknown symbol: $this")
}

fun FirReference.toSymbolForCall(
    session: FirSession,
    classifierStorage: Fir2IrClassifierStorage,
    declarationStorage: Fir2IrDeclarationStorage,
    conversionScope: Fir2IrConversionScope,
    preferGetter: Boolean = true
): IrSymbol? {
    return when (this) {
        is FirResolvedNamedReference -> resolvedSymbol.toSymbolForCall(session, classifierStorage, declarationStorage, preferGetter)
        is FirErrorNamedReference -> candidateSymbol?.toSymbolForCall(session, classifierStorage, declarationStorage, preferGetter)
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

private fun FirCallableSymbol<*>.toSymbolForCall(declarationStorage: Fir2IrDeclarationStorage, preferGetter: Boolean): IrSymbol? =
    when (this) {
        is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(this)
        is FirSyntheticPropertySymbol -> {
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
        is FirDelegateFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(this)
        is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(this)
        else -> null
    }

fun FirConstExpression<*>.getIrConstKind(): IrConstKind<*> = when (kind) {
    ConstantValueKind.IntegerLiteral -> {
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

private fun ConstantValueKind<*>.toIrConstKind(): IrConstKind<*> = when (this) {
    ConstantValueKind.Null -> IrConstKind.Null
    ConstantValueKind.Boolean -> IrConstKind.Boolean
    ConstantValueKind.Char -> IrConstKind.Char

    ConstantValueKind.Byte -> IrConstKind.Byte
    ConstantValueKind.Short -> IrConstKind.Short
    ConstantValueKind.Int -> IrConstKind.Int
    ConstantValueKind.Long -> IrConstKind.Long

    ConstantValueKind.UnsignedByte -> IrConstKind.Byte
    ConstantValueKind.UnsignedShort -> IrConstKind.Short
    ConstantValueKind.UnsignedInt -> IrConstKind.Int
    ConstantValueKind.UnsignedLong -> IrConstKind.Long

    ConstantValueKind.String -> IrConstKind.String
    ConstantValueKind.Float -> IrConstKind.Float
    ConstantValueKind.Double -> IrConstKind.Double
    ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> throw IllegalArgumentException()
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

    if (fir.isIntersectionOverride) {
        // We've got IR declarations (fake overrides) for intersection overrides in classes, but not for intersection types
        // interface A { fun foo() }
        // interface B { fun foo() }
        // interface C : A, B // for C.foo we've got an IR fake override
        // for {A & B} we don't have such an IR declaration, so we're unwrapping it
        if (fir.dispatchReceiverType is ConeIntersectionType) {
            return fir.baseForIntersectionOverride!!.symbol.unwrapCallRepresentative(this)
        }

        return this
    }

    val overriddenSymbol = fir.originalForSubstitutionOverride?.takeIf {
        it.containingClass() == root.containingClass()
    }?.symbol ?: return this

    return overriddenSymbol.unwrapCallRepresentative(this)
}

internal fun FirSimpleFunction.generateOverriddenFunctionSymbols(
    containingClass: FirClass,
    session: FirSession,
    scopeSession: ScopeSession,
    declarationStorage: Fir2IrDeclarationStorage,
    fakeOverrideGenerator: FakeOverrideGenerator,
): List<IrSimpleFunctionSymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses(declarationStorage) ?: return emptyList()

    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
    scope.processFunctionsByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    scope.processOverriddenFunctionsFromSuperClasses(symbol, containingClass) {
        if (it.fir.visibility == Visibilities.Private) {
            return@processOverriddenFunctionsFromSuperClasses ProcessorAction.NEXT
        }

        for (overridden in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            overriddenSet += overridden
        }

        ProcessorAction.NEXT
    }

    return overriddenSet.toList()
}

fun FirTypeScope.processOverriddenFunctionsFromSuperClasses(
    functionSymbol: FirNamedFunctionSymbol,
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction = processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { overridden, baseScope ->
    if (overridden.containingClass() == containingClass.symbol.toLookupTag()) {
        baseScope.processOverriddenFunctionsFromSuperClasses(overridden, containingClass, processor)
    } else {
        processor(overridden)
    }
}

fun FirTypeScope.processOverriddenPropertiesFromSuperClasses(
    propertySymbol: FirPropertySymbol,
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction = processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { overridden, baseScope ->
    if (overridden.containingClass() == containingClass.symbol.toLookupTag()) {
        baseScope.processOverriddenPropertiesFromSuperClasses(overridden, containingClass, processor)
    } else {
        processor(overridden)
    }
}

private fun FirClass.getSuperTypesAsIrClasses(
    declarationStorage: Fir2IrDeclarationStorage
): Set<IrClass>? {
    val irClass =
        declarationStorage.classifierStorage.getIrClassSymbol(symbol).owner as? IrClass ?: return null

    return irClass.superTypes.mapNotNull { it.classifierOrNull?.owner as? IrClass }.toSet()
}

internal fun FirProperty.generateOverriddenPropertySymbols(
    containingClass: FirClass,
    session: FirSession,
    scopeSession: ScopeSession,
    declarationStorage: Fir2IrDeclarationStorage,
    fakeOverrideGenerator: FakeOverrideGenerator,
): List<IrPropertySymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses(declarationStorage) ?: return emptyList()

    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrPropertySymbol>()
    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) {
        if (it.fir.visibility == Visibilities.Private) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }

        for (overridden in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            overriddenSet += overridden
        }

        ProcessorAction.NEXT
    }

    return overriddenSet.toList()
}

internal fun FirProperty.generateOverriddenAccessorSymbols(
    containingClass: FirClass,
    isGetter: Boolean,
    session: FirSession,
    scopeSession: ScopeSession,
    declarationStorage: Fir2IrDeclarationStorage,
    fakeOverrideGenerator: FakeOverrideGenerator,
): List<IrSimpleFunctionSymbol> {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    val superClasses = containingClass.getSuperTypesAsIrClasses(declarationStorage) ?: return emptyList()

    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) {
        if (it.fir.visibility == Visibilities.Private) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }

        for (overriddenProperty in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            val overriddenAccessor = if (isGetter) overriddenProperty.owner.getter?.symbol else overriddenProperty.owner.setter?.symbol
            if (overriddenAccessor != null) {
                overriddenSet += overriddenAccessor
            }
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
            is AccessorSymbol, is FirSyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
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
): IrValueParameter =
    symbolTable.irFactory.createValueParameter(
        startOffset, endOffset, thisOrigin, IrValueParameterSymbolImpl(),
        Name.special("<this>"), UNDEFINED_PARAMETER_INDEX, thisType,
        varargElementType = null, isCrossinline = false, isNoinline = false,
        isHidden = false, isAssignable = false
    ).apply {
        this.parent = this@declareThisReceiverParameter
    }

fun FirClass.irOrigin(firProvider: FirProvider): IrDeclarationOrigin = when {
    firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    isJava -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
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

// TODO: implement inlineClassRepresentation in FirRegularClass instead.
fun Fir2IrComponents.computeInlineClassRepresentation(klass: FirRegularClass): InlineClassRepresentation<IrSimpleType>? {
    if (!klass.isInline) return null
    val parameter = klass.getInlineClassUnderlyingParameter() ?: error("Inline class has no underlying parameter: ${klass.render()}")
    val underlyingType = parameter.returnTypeRef.toIrType(typeConverter)
    return InlineClassRepresentation(
        parameter.name,
        underlyingType as? IrSimpleType ?: error("Inline class underlying type is not a simple type: ${klass.render()}")
    )
}
