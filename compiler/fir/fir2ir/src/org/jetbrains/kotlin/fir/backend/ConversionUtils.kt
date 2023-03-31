/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizationResult
import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COMPONENT_PREFIX
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.startOffsetSkippingComments
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private fun AbstractKtSourceElement?.startOffsetSkippingComments(): Int? {
    return when (this) {
        is KtPsiSourceElement -> this.psi.startOffsetSkippingComments
        is KtLightSourceElement -> this.startOffsetSkippingComments
        else -> null
    }
}

internal fun <T : IrElement> FirElement.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    return source.convertWithOffsets(f)
}

internal fun <T : IrElement> KtSourceElement?.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    val startOffset = this?.startOffsetSkippingComments() ?: this?.startOffset ?: UNDEFINED_OFFSET
    val endOffset = this?.endOffset ?: UNDEFINED_OFFSET
    return f(startOffset, endOffset)
}

internal fun <T : IrElement> FirQualifiedAccessExpression.convertWithOffsets(
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    return convertWithOffsets(this.calleeReference, f)
}

internal fun <T : IrElement> FirStatement.convertWithOffsets(
    calleeReference: FirReference,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (psi is PsiCompiledElement) return f(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
    val startOffset = calleeReference.source?.startOffsetSkippingComments() ?: calleeReference.source?.startOffset ?: UNDEFINED_OFFSET
    val endOffset = source?.endOffset ?: UNDEFINED_OFFSET
    return f(startOffset, endOffset)
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

internal enum class ConversionTypeOrigin {
    DEFAULT,
    SETTER
}

class ConversionTypeContext internal constructor(internal val origin: ConversionTypeOrigin) {
    fun inSetter() = ConversionTypeContext(
        origin = ConversionTypeOrigin.SETTER
    )

    companion object {
        internal val DEFAULT = ConversionTypeContext(
            origin = ConversionTypeOrigin.DEFAULT
        )
        internal val IN_SETTER = ConversionTypeContext(
            origin = ConversionTypeOrigin.SETTER
        )
    }
}

context(Fir2IrComponents)
fun FirClassifierSymbol<*>.toSymbol(
    typeContext: ConversionTypeContext = ConversionTypeContext.DEFAULT,
    handleAnnotations: ((List<FirAnnotation>) -> Unit)? = null
): IrClassifierSymbol {
    return when (this) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(this, typeContext)
        }

        is FirTypeAliasSymbol -> {
            handleAnnotations?.invoke(fir.expandedTypeRef.annotations)
            val coneClassLikeType = fir.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)
                ?.toSymbol(typeContext, handleAnnotations)
                ?: classifierStorage.getIrClassSymbolForNotFoundClass(coneClassLikeType.lookupTag)
        }

        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(this)
        }

        else -> error("Unknown symbol: $this")
    }
}

context(Fir2IrComponents)
private fun FirBasedSymbol<*>.toSymbolForCall(
    dispatchReceiver: FirExpression,
    preferGetter: Boolean,
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? = when (this) {
    is FirCallableSymbol<*> -> unwrapCallRepresentative().toSymbolForCall(
        dispatchReceiver,
        preferGetter,
        explicitReceiver,
        isDelegate,
        isReference
    )

    is FirClassifierSymbol<*> -> toSymbol()
    else -> error("Unknown symbol: $this")
}

context(Fir2IrComponents)
fun FirReference.toSymbolForCall(
    dispatchReceiver: FirExpression,
    conversionScope: Fir2IrConversionScope,
    explicitReceiver: FirExpression?,
    preferGetter: Boolean = true,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? {
    return when (this) {
        is FirResolvedNamedReference ->
            resolvedSymbol.toSymbolForCall(
                dispatchReceiver,
                preferGetter,
                explicitReceiver,
                isDelegate,
                isReference
            )

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

private fun FirResolvedQualifier.toLookupTag(session: FirSession): ConeClassLikeLookupTag? =
    when (val symbol = symbol) {
        is FirClassSymbol -> symbol.toLookupTag()
        is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.toLookupTag()
        else -> null
    }

context(Fir2IrComponents)
private fun FirCallableSymbol<*>.toSymbolForCall(
    dispatchReceiver: FirExpression,
    preferGetter: Boolean,
    // Note: in fact LHS for callable references and explicit receiver for normal qualified accesses
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? {
    val fakeOverrideOwnerLookupTag = when {
        // Static fake overrides
        isStatic -> {
            (dispatchReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        // Member fake override or bound callable reference
        dispatchReceiver !is FirNoReceiverExpression -> {
            val callSiteDispatchReceiverType = dispatchReceiver.typeRef.coneType
            val declarationSiteDispatchReceiverType = dispatchReceiverType
            val type = if (callSiteDispatchReceiverType is ConeDynamicType && declarationSiteDispatchReceiverType != null) {
                declarationSiteDispatchReceiverType
            } else {
                callSiteDispatchReceiverType
            }
            type.findClassRepresentation(type, declarationStorage.session)
        }
        // Unbound callable reference to member (non-extension)
        isReference && fir.receiverParameter == null -> {
            // TODO: remove runIf with StandardClassIds.Any comparison after fixing ValueClass::equals case (KT-54887)
            runIf(containingClassLookupTag()?.classId != StandardClassIds.Any) {
                (explicitReceiver as? FirResolvedQualifier)?.toLookupTag(session)
            }
        }
        else -> null
    }

    return when (this) {
        is FirSimpleSyntheticPropertySymbol -> {
            if (isDelegate) {
                declarationStorage.getIrPropertySymbol(this)
            } else {
                (fir as? FirSyntheticProperty)?.let { syntheticProperty ->
                    if (isReference) {
                        declarationStorage.getIrPropertySymbol(this, fakeOverrideOwnerLookupTag)
                    } else {
                        val delegateSymbol = if (preferGetter) {
                            syntheticProperty.getter.delegate.symbol
                        } else {
                            syntheticProperty.setter?.delegate?.symbol
                                ?: throw AssertionError("Written synthetic property must have a setter")
                        }
                        delegateSymbol.unwrapCallRepresentative()
                            .toSymbolForCall(dispatchReceiver, preferGetter, isDelegate = false)
                    }
                } ?: declarationStorage.getIrPropertySymbol(this)
            }
        }

        is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(this, fakeOverrideOwnerLookupTag)
        is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(this, fakeOverrideOwnerLookupTag)
        is FirFieldSymbol -> declarationStorage.getIrFieldSymbol(this, fakeOverrideOwnerLookupTag)
        is FirBackingFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(this)
        is FirDelegateFieldSymbol -> declarationStorage.getIrDelegateFieldSymbol(this)
        is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(this)
        else -> null
    }
}

fun FirConstExpression<*>.getIrConstKind(): IrConstKind<*> = when (kind) {
    ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> {
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
            // Strip all annotations (including special annotations such as @EnhancedNullability) from constant type
            irType.removeAnnotations(),
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
    ConstantValueKind.Error -> throw IllegalArgumentException()
}


internal tailrec fun FirCallableSymbol<*>.unwrapSubstitutionAndIntersectionOverrides(): FirCallableSymbol<*> {
    val originalForSubstitutionOverride = originalForSubstitutionOverride
    if (originalForSubstitutionOverride != null && originalForSubstitutionOverride != this) {
        return originalForSubstitutionOverride.unwrapSubstitutionAndIntersectionOverrides()
    }

    val baseForIntersectionOverride = baseForIntersectionOverride
    if (baseForIntersectionOverride != null) return baseForIntersectionOverride.unwrapSubstitutionAndIntersectionOverrides()

    return this
}

context(Fir2IrComponents)
internal tailrec fun FirCallableSymbol<*>.unwrapCallRepresentative(
    owner: ConeClassLikeLookupTag? = containingClassLookupTag()
): FirCallableSymbol<*> {
    val fir = fir

    if (fir is FirConstructor) {
        val originalForTypeAlias = fir.originalConstructorIfTypeAlias
        if (originalForTypeAlias != null) {
            return originalForTypeAlias.symbol.unwrapCallRepresentative(owner)
        }
    }

    if (fir.isIntersectionOverride) {
        // We've got IR declarations (fake overrides) for intersection overrides in classes, but not for intersection types
        // interface A { fun foo() }
        // interface B { fun foo() }
        // interface C : A, B // for C.foo we've got an IR fake override
        // for {A & B} we don't have such an IR declaration, so we're unwrapping it
        if (fir.dispatchReceiverType is ConeIntersectionType) {
            return fir.baseForIntersectionOverride!!.symbol.unwrapCallRepresentative(owner)
        }

        return this
    }

    val originalForOverride = fir.originalForSubstitutionOverride
    if (originalForOverride != null && originalForOverride.containingClassLookupTag() == owner) {
        return originalForOverride.symbol.unwrapCallRepresentative(owner)
    }

    return this
}

context(Fir2IrComponents)
internal fun FirSimpleFunction.processOverriddenFunctionSymbols(
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> Unit
) {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
    scope.processFunctionsByName(name) {}
    scope.processOverriddenFunctionsFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenFunctionsFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }
}

context(Fir2IrComponents)
internal fun FirSimpleFunction.generateOverriddenFunctionSymbols(containingClass: FirClass): List<IrSimpleFunctionSymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses() ?: return emptyList()
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()

    processOverriddenFunctionSymbols(containingClass) {
        for (overridden in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            overriddenSet += overridden
        }
    }

    return overriddenSet.toList()
}

fun FirTypeScope.processOverriddenFunctionsFromSuperClasses(
    functionSymbol: FirNamedFunctionSymbol,
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction =
    processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride &&
            overridden.dispatchReceiverClassLookupTagOrNull() == containingClass.symbol.toLookupTag()
        )
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }

fun FirTypeScope.processOverriddenPropertiesFromSuperClasses(
    propertySymbol: FirPropertySymbol,
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction =
    processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride &&
            overridden.dispatchReceiverClassLookupTagOrNull() == containingClass.symbol.toLookupTag()
        )
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }

context(Fir2IrComponents)
private fun FirClass.getSuperTypesAsIrClasses(): Set<IrClass>? {
    val irClass =
        declarationStorage.classifierStorage.getIrClassSymbol(symbol).owner as? IrClass ?: return null

    return irClass.superTypes.mapNotNull { it.classifierOrNull?.owner as? IrClass }.toSet()
}

context(Fir2IrComponents)
internal fun FirProperty.processOverriddenPropertySymbols(
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> Unit
): List<IrPropertySymbol> {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrPropertySymbol>()
    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }

    return overriddenSet.toList()
}

context(Fir2IrComponents)
internal fun FirProperty.generateOverriddenPropertySymbols(containingClass: FirClass): List<IrPropertySymbol> {
    val superClasses = containingClass.getSuperTypesAsIrClasses() ?: return emptyList()
    val overriddenSet = mutableSetOf<IrPropertySymbol>()

    processOverriddenPropertySymbols(containingClass) {
        for (overridden in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)) {
            overriddenSet += overridden
        }
    }

    return overriddenSet.toList()
}

context(Fir2IrComponents)
internal fun FirProperty.generateOverriddenAccessorSymbols(containingClass: FirClass, isGetter: Boolean): List<IrSimpleFunctionSymbol> {
    val scope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
    scope.processPropertiesByName(name) {}
    val overriddenSet = mutableSetOf<IrSimpleFunctionSymbol>()
    val superClasses = containingClass.getSuperTypesAsIrClasses() ?: return emptyList()

    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }

        for (overriddenIrPropertySymbol in fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(overriddenSymbol, superClasses)) {
            val overriddenIrAccessorSymbol =
                if (isGetter) overriddenIrPropertySymbol.owner.getter?.symbol
                else overriddenIrPropertySymbol.owner.setter?.symbol
            if (overriddenIrAccessorSymbol != null) {
                overriddenSet += overriddenIrAccessorSymbol
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
    OperatorNameConventions.RANGE_TO to IrStatementOrigin.RANGE,
    OperatorNameConventions.RANGE_UNTIL to IrStatementOrigin.RANGE_UNTIL,
    OperatorNameConventions.CONTAINS to IrStatementOrigin.IN,
)

internal fun FirReference.statementOrigin(): IrStatementOrigin? = when (this) {
    is FirPropertyFromParameterResolvedNamedReference -> IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
    is FirResolvedNamedReference -> when (val symbol = resolvedSymbol) {
        is FirSyntheticPropertySymbol -> IrStatementOrigin.GET_PROPERTY
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

            source?.kind is KtFakeSourceElementKind.DesugaredComponentFunctionCall ->
                IrStatementOrigin.COMPONENT_N.withIndex(name.asString().removePrefix(DATA_CLASS_COMPONENT_PREFIX).toInt())

            else ->
                null
        }

        else -> null
    }

    else -> null
}

context(Fir2IrComponents)
internal fun IrDeclarationParent.declareThisReceiverParameter(
    thisType: IrType,
    thisOrigin: IrDeclarationOrigin,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = SpecialNames.THIS,
    explicitReceiver: FirReceiverParameter? = null,
): IrValueParameter {
    return symbolTable.irFactory.createValueParameter(
        startOffset, endOffset, thisOrigin, IrValueParameterSymbolImpl(),
        name, UNDEFINED_PARAMETER_INDEX, thisType,
        varargElementType = null, isCrossinline = false, isNoinline = false,
        isHidden = false, isAssignable = false
    ).apply {
        this.parent = this@declareThisReceiverParameter
        explicitReceiver?.let { annotationGenerator.generate(this, it) }
    }
}

fun FirClass.irOrigin(firProvider: FirProvider): IrDeclarationOrigin = when {
    firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    isJava -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    else -> when (val origin = origin) {
        is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(origin.key)
        else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    }
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

fun Fir2IrComponents.computeValueClassRepresentation(klass: FirRegularClass): ValueClassRepresentation<IrSimpleType>? {
    require((klass.valueClassRepresentation != null) == klass.isInline) {
        "Value class has no representation: ${klass.render()}"
    }
    return klass.valueClassRepresentation?.mapUnderlyingType {
        with(typeConverter) {
            it.toIrType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${klass.render()}")
        }
    }
}

context(Fir2IrComponents)
fun FirRegularClass.getIrSymbolsForSealedSubclasses(): List<IrClassSymbol> {
    val symbolProvider = session.symbolProvider
    return getSealedClassInheritors(session).mapNotNull {
        symbolProvider.getClassLikeSymbolByClassId(it)?.toSymbol()
    }.filterIsInstance<IrClassSymbol>()
}

@OptIn(FirExtensionApiInternals::class)
fun FirSession.createFilesWithGeneratedDeclarations(): List<FirFile> {
    val symbolProvider = generatedDeclarationsSymbolProvider ?: return emptyList()
    val declarationGenerators = extensionService.declarationGenerators
    val topLevelClasses = declarationGenerators.flatMap { it.topLevelClassIdsCache.getValue() }.groupBy { it.packageFqName }
    val topLevelCallables = declarationGenerators.flatMap { it.topLevelCallableIdsCache.getValue() }.groupBy { it.packageName }

    return buildList {
        for (packageFqName in (topLevelClasses.keys + topLevelCallables.keys)) {
            this += buildFile {
                symbol = FirFileSymbol()
                annotationsContainer = buildFileAnnotationsContainer {
                    moduleData = this@createFilesWithGeneratedDeclarations.moduleData
                    containingFileSymbol = this@buildFile.symbol
                }
                origin = FirDeclarationOrigin.Synthetic
                moduleData = this@createFilesWithGeneratedDeclarations.moduleData
                packageDirective = buildPackageDirective {
                    this.packageFqName = packageFqName
                }
                name = "__GENERATED DECLARATIONS__.kt"
                declarations += topLevelCallables.getOrDefault(packageFqName, emptyList())
                    .flatMap { symbolProvider.getTopLevelCallableSymbols(packageFqName, it.callableName) }
                    .map { it.fir }
                declarations += topLevelClasses.getOrDefault(packageFqName, emptyList())
                    .mapNotNull { symbolProvider.getClassLikeSymbolByClassId(it)?.fir }
            }
        }
    }
}

fun FirDeclaration?.computeIrOrigin(predefinedOrigin: IrDeclarationOrigin? = null): IrDeclarationOrigin {
    return predefinedOrigin
        ?: (this?.origin as? FirDeclarationOrigin.Plugin)?.let { GeneratedByPlugin(it.key) }
        ?: (this as? FirValueParameter)?.name?.let {
            when (it) {
                SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> IrDeclarationOrigin.UNDERSCORE_PARAMETER
                SpecialNames.DESTRUCT -> IrDeclarationOrigin.DESTRUCTURED_OBJECT_PARAMETER
                else -> null
            }
        }
        ?: IrDeclarationOrigin.DEFINED
}

fun FirVariableAssignment.getIrAssignmentOrigin(): IrStatementOrigin {
    val calleeReferenceSymbol = calleeReference?.toResolvedCallableSymbol() ?: return IrStatementOrigin.EQ
    val rValue = rValue
    if (rValue is FirFunctionCall && calleeReferenceSymbol.callableId.isLocal) {
        val callableId = rValue.calleeReference.toResolvedCallableSymbol()?.callableId
        if (callableId?.classId == StandardClassIds.Int) {
            val callableName = callableId.callableName
            if (callableName == OperatorNameConventions.INC) {
                return if (source?.elementType == KtNodeTypes.PREFIX_EXPRESSION)
                    IrStatementOrigin.PREFIX_INCR
                else
                    IrStatementOrigin.POSTFIX_INCR
            } else if (callableName == OperatorNameConventions.DEC) {
                return if (source?.elementType == KtNodeTypes.PREFIX_EXPRESSION)
                    IrStatementOrigin.PREFIX_DECR
                else
                    IrStatementOrigin.POSTFIX_DECR
            }

            val kind = rValue.source?.kind
            if (kind == KtFakeSourceElementKind.DesugaredIncrementOrDecrement || kind == KtFakeSourceElementKind.DesugaredCompoundAssignment) {
                if (callableName == OperatorNameConventions.PLUS) {
                    return IrStatementOrigin.PLUSEQ
                } else if (callableName == OperatorNameConventions.MINUS) {
                    return IrStatementOrigin.MINUSEQ
                }
            }
        }
    }
    return IrStatementOrigin.EQ
}

fun FirCallableDeclaration.contextReceiversForFunctionOrContainingProperty(): List<FirContextReceiver> =
    if (this is FirPropertyAccessor)
        this.propertySymbol.fir.contextReceivers
    else
        this.contextReceivers

fun IrActualizationResult?.extractFirDeclarations(): Set<FirDeclaration>? {
    return this?.actualizedExpectDeclarations?.mapNotNullTo(mutableSetOf()) { ((it as IrMetadataSourceOwner).metadata as FirMetadataSource).fir }
}
