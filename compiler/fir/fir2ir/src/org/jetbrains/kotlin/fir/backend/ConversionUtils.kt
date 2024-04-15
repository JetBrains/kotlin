/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COMPONENT_PREFIX
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.diagnostics.startOffsetSkippingComments
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isJava
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun AbstractKtSourceElement?.startOffsetSkippingComments(): Int? {
    return when (this) {
        is KtPsiSourceElement -> this.psi.startOffsetSkippingComments
        is KtLightSourceElement -> this.startOffsetSkippingComments
        else -> null
    }
}

internal inline fun <T : IrElement> FirElement.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    return source.convertWithOffsets(f)
}

internal fun <T : IrElement> FirPropertyAccessor?.convertWithOffsets(
    defaultStartOffset: Int, defaultEndOffset: Int, f: (startOffset: Int, endOffset: Int) -> T
): T {
    return if (this == null) return f(defaultStartOffset, defaultEndOffset) else source.convertWithOffsets(f)
}

internal inline fun <T : IrElement> KtSourceElement?.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    val startOffset: Int
    val endOffset: Int

    if (
        isCompiledElement(psi) ||
        this?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers ||
        this?.kind == KtFakeSourceElementKind.ImplicitThisReceiverExpression
    ) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = this?.startOffsetSkippingComments() ?: this?.startOffset ?: UNDEFINED_OFFSET
        endOffset = this?.endOffset ?: UNDEFINED_OFFSET
    }

    return f(startOffset, endOffset)
}

internal fun <T : IrElement> FirQualifiedAccessExpression.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    if (shouldUseCalleeReferenceAsItsSourceInIr()) {
        return convertWithOffsets(calleeReference, f)
    }
    return (this as FirElement).convertWithOffsets(f)
}

/**
 * This function determines which source should be used for IR counterpart of this FIR expression.
 *
 * At the moment, this function reproduces (~) K1 logic.
 * Currently, K1 uses full qualified expression source (from receiver to selector)
 * in case of an operator call, an infix call, a callable reference, or a referenced class/object.
 * Otherwise, only selector is used as a source.
 *
 * See also KT-60111 about an operator call case (xxx + yyy).
 */
fun FirQualifiedAccessExpression.shouldUseCalleeReferenceAsItsSourceInIr(): Boolean {
    return when {
        this is FirImplicitInvokeCall -> true
        this is FirFunctionCall && origin != FirFunctionCallOrigin.Regular -> false
        this is FirCallableReferenceAccess -> false
        else -> (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol is FirCallableSymbol
    }
}

internal inline fun <T : IrElement> FirThisReceiverExpression.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    return source.convertWithOffsets(f)
}

internal inline fun <T : IrElement> FirStatement.convertWithOffsets(
    calleeReference: FirReference,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val startOffset: Int
    val endOffset: Int
    if (isCompiledElement(psi)) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = calleeReference.source?.startOffsetSkippingComments() ?: calleeReference.source?.startOffset ?: UNDEFINED_OFFSET
        endOffset = source?.endOffset ?: UNDEFINED_OFFSET
    }
    return f(startOffset, endOffset)
}

private fun isCompiledElement(element: PsiElement?): Boolean {
    if (element == null) {
        return false
    }

    if (element is PsiCompiledElement) {
        return true
    }

    val containingFile = element.containingFile
    return containingFile !is KtFile || containingFile.isCompiled
}

internal fun createErrorType(): IrErrorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)

enum class ConversionTypeOrigin(val forSetter: Boolean) {
    DEFAULT(forSetter = false),
    SETTER(forSetter = true);
}

fun FirClassifierSymbol<*>.toSymbol(
    c: Fir2IrComponents,
    typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
    handleAnnotations: ((List<FirAnnotation>) -> Unit)? = null
): IrClassifierSymbol = with(c) {
    val symbol = this@toSymbol
    when (symbol) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(symbol, typeOrigin)
        }

        is FirTypeAliasSymbol -> {
            handleAnnotations?.invoke(symbol.fir.expandedTypeRef.annotations)
            val coneClassLikeType = symbol.fir.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)
                ?.toSymbol(c, typeOrigin, handleAnnotations)
                ?: classifiersGenerator.createIrClassForNotFoundClass(coneClassLikeType.lookupTag).symbol
        }

        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(symbol)
        }

        else -> error("Unknown symbol: $symbol")
    }
}

private fun FirBasedSymbol<*>.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    preferGetter: Boolean,
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? = when (this) {
    is FirCallableSymbol<*> -> toSymbolForCall(
        c,
        dispatchReceiver,
        preferGetter,
        explicitReceiver,
        isDelegate,
        isReference
    )

    is FirClassifierSymbol<*> -> toSymbol(c)
    else -> error("Unknown symbol: $this")
}

fun FirReference.extractSymbolForCall(c: Fir2IrComponents): FirBasedSymbol<*>? {
    if (this !is FirResolvedNamedReference) {
        return null
    }
    var symbol = resolvedSymbol

    if (symbol is FirCallableSymbol<*>) {
        if (symbol.origin == FirDeclarationOrigin.SubstitutionOverride.CallSite) {
            symbol = symbol.fir.unwrapUseSiteSubstitutionOverrides<FirCallableDeclaration>().symbol
        }
        @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
        symbol = (symbol as FirCallableSymbol<*>).unwrapCallRepresentative(c)
    }
    return symbol
}

@OptIn(ExperimentalContracts::class)
fun FirReference.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    explicitReceiver: FirExpression?,
    preferGetter: Boolean = true,
    isDelegate: Boolean = false,
    isReference: Boolean = false,
): IrSymbol? {
    contract {
        returnsNotNull() implies (this@toSymbolForCall is FirResolvedNamedReference)
    }

    return extractSymbolForCall(c)?.toSymbolForCall(
        c,
        dispatchReceiver,
        preferGetter,
        explicitReceiver,
        isDelegate,
        isReference
    )
}

private fun FirResolvedQualifier.toLookupTag(session: FirSession): ConeClassLikeLookupTag? =
    when (val symbol = symbol) {
        is FirClassSymbol -> symbol.toLookupTag()
        is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.toLookupTag()
        else -> null
    }

fun FirCallableSymbol<*>.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    preferGetter: Boolean = true,
    // Note: in fact LHS for callable references and explicit receiver for normal qualified accesses
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? = with(c) {
    val fakeOverrideOwnerLookupTag = when {
        // Static fake overrides
        isStatic -> {
            (dispatchReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        // Member fake override or bound callable reference
        dispatchReceiver != null -> {
            val callSiteDispatchReceiverType = when (dispatchReceiver) {
                is FirSmartCastExpression -> dispatchReceiver.smartcastTypeWithoutNullableNothing?.coneType ?: dispatchReceiver.resolvedType
                else -> dispatchReceiver.resolvedType
            }
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
            (explicitReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        else -> null
    }
    return when (val symbol =  this@toSymbolForCall) {
        is FirSimpleSyntheticPropertySymbol -> {
            if (isDelegate) {
                declarationStorage.getIrPropertySymbol(symbol)
            } else {
                (fir as? FirSyntheticProperty)?.let { syntheticProperty ->
                    if (isReference) {
                        declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
                    } else {
                        val delegateSymbol = if (preferGetter) {
                            syntheticProperty.getter.delegate.symbol
                        } else {
                            syntheticProperty.setter?.delegate?.symbol
                                ?: throw AssertionError("Written synthetic property must have a setter")
                        }
                        delegateSymbol.unwrapCallRepresentative(c)
                            .toSymbolForCall(c, dispatchReceiver, preferGetter, isDelegate = false)
                    }
                } ?: declarationStorage.getIrPropertySymbol(symbol)
            }
        }
        is FirConstructorSymbol -> declarationStorage.getIrConstructorSymbol(symbol.fir.originalConstructorIfTypeAlias?.symbol ?: symbol)
        is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirFieldSymbol -> declarationStorage.getOrCreateIrField(symbol, fakeOverrideOwnerLookupTag).symbol
        is FirBackingFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(symbol)
        is FirDelegateFieldSymbol -> declarationStorage.getIrDelegateFieldSymbol(symbol)
        is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(symbol)
        else -> null
    }
}

fun FirLiteralExpression<*>.getIrConstKind(): IrConstKind<*> = when (kind) {
    ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> {
        val type = resolvedType as ConeIntegerLiteralType
        type.getApproximatedType().toConstKind()!!.toIrConstKind()
    }

    else -> kind.toIrConstKind()
}

fun <T> FirLiteralExpression<T>.toIrConst(irType: IrType): IrConst<T> {
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

internal tailrec fun FirCallableSymbol<*>.unwrapCallRepresentative(
    c: Fir2IrComponents,
    owner: ConeClassLikeLookupTag? = containingClassLookupTag()
): FirCallableSymbol<*> {
    val fir = fir

    if (fir is FirConstructor) {
        val originalForTypeAlias = fir.originalConstructorIfTypeAlias
        if (originalForTypeAlias != null) {
            return originalForTypeAlias.symbol.unwrapCallRepresentative(c, owner)
        }
    }

    if (fir.isIntersectionOverride) {
        // We've got IR declarations (fake overrides) for intersection overrides in classes, but not for intersection types
        // interface A { fun foo() }
        // interface B { fun foo() }
        // interface C : A, B // for C.foo we've got an IR fake override
        // for {A & B} we don't have such an IR declaration, so we're unwrapping it
        if (fir.dispatchReceiverType is ConeIntersectionType) {
            return fir.baseForIntersectionOverride!!.symbol.unwrapCallRepresentative(c, owner)
        }

        return this
    }

    val originalForOverride = fir.originalForSubstitutionOverride
    if (originalForOverride != null && originalForOverride.containingClassLookupTag() == owner) {
        return originalForOverride.symbol.unwrapCallRepresentative(c, owner)
    }

    return this
}

internal fun FirSimpleFunction.processOverriddenFunctionSymbols(
    containingClass: FirClass,
    c: Fir2IrComponents,
    processor: (FirNamedFunctionSymbol) -> Unit
) {
    val scope = containingClass.unsubstitutedScope(c)
    scope.processFunctionsByName(name) {}
    scope.processOverriddenFunctionsFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!c.session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenFunctionsFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }
}

fun FirTypeScope.processOverriddenFunctionsFromSuperClasses(
    functionSymbol: FirNamedFunctionSymbol,
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction {
    val ownerTag = containingClass.symbol.toLookupTag()
    return processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride && ownerTag.isRealOwnerOf(overridden))
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }
}

fun FirTypeScope.processOverriddenPropertiesFromSuperClasses(
    propertySymbol: FirPropertySymbol,
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction {
    val ownerTag = containingClass.symbol.toLookupTag()
    return processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride && ownerTag.isRealOwnerOf(overridden))
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }
}

internal fun FirProperty.processOverriddenPropertySymbols(
    containingClass: FirClass,
    c: Fir2IrComponents,
    processor: (FirPropertySymbol) -> Unit
) {
    val scope = containingClass.unsubstitutedScope(c)
    scope.processPropertiesByName(name) {}
    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!c.session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }
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

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIteratorNext() ->
                IrStatementOrigin.FOR_LOOP_NEXT

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIteratorHasNext() ->
                IrStatementOrigin.FOR_LOOP_HAS_NEXT

            source?.kind == KtFakeSourceElementKind.DesugaredForLoop && symbol.callableId.isIterator() ->
                IrStatementOrigin.FOR_LOOP_ITERATOR

            source?.kind == KtFakeSourceElementKind.DesugaredInvertedContains ->
                IrStatementOrigin.NOT_IN

            source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement ->
                incOrDeclSourceKindToIrStatementOrigin[source?.kind]

            source?.kind is KtFakeSourceElementKind.DesugaredPrefixSecondGetReference ->
                incOrDeclSourceKindToIrStatementOrigin[source?.kind]

            source?.elementType == KtNodeTypes.OPERATION_REFERENCE ->
                nameToOperationConventionOrigin[symbol.callableId.callableName]

            source?.kind is KtFakeSourceElementKind.DesugaredComponentFunctionCall ->
                IrStatementOrigin.COMPONENT_N.withIndex(name.asString().removePrefix(DATA_CLASS_COMPONENT_PREFIX).toInt())

            source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign ->
                augmentedAssignSourceKindToIrStatementOrigin[source?.kind]

            source?.kind is KtFakeSourceElementKind.ArrayAccessNameReference -> when (name) {
                OperatorNameConventions.GET -> IrStatementOrigin.GET_ARRAY_ELEMENT
                OperatorNameConventions.SET -> IrStatementOrigin.EQ
                else -> null
            }

            else ->
                null
        }

        else -> null
    }

    else -> null
}

internal fun IrDeclarationParent.declareThisReceiverParameter(
    c: Fir2IrComponents,
    thisType: IrType,
    thisOrigin: IrDeclarationOrigin,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    name: Name = SpecialNames.THIS,
    explicitReceiver: FirReceiverParameter? = null,
    isAssignable: Boolean = false
): IrValueParameter {
    return c.irFactory.createValueParameter(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = thisOrigin,
        name = name,
        type = thisType,
        isAssignable = isAssignable,
        symbol = IrValueParameterSymbolImpl(),
        index = UNDEFINED_PARAMETER_INDEX,
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
    ).apply {
        this.parent = this@declareThisReceiverParameter
        explicitReceiver?.let { c.annotationGenerator.generate(this, it) }
    }
}

fun FirClass.irOrigin(c: Fir2IrComponents): IrDeclarationOrigin = when {
    c.firProvider.getFirClassifierContainerFileIfAny(symbol) != null -> IrDeclarationOrigin.DEFINED
    isJava -> IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
    else -> when (val origin = origin) {
        is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(origin.key)
        else -> IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    }
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
    val receiverVariable = callablesGenerator.declareTemporaryVariable(receiverExpression, nameHint).apply {
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
        it.toIrType(this) as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${klass.render()}")
    }
}

fun FirRegularClass.getIrSymbolsForSealedSubclasses(c: Fir2IrComponents): List<IrClassSymbol> {
    val symbolProvider = c.session.symbolProvider
    return getSealedClassInheritors(c.session).mapNotNull {
        symbolProvider.getClassLikeSymbolByClassId(it)?.toSymbol(c)
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
                origin = FirDeclarationOrigin.Synthetic.PluginFile
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

fun FirDeclaration?.computeIrOrigin(
    predefinedOrigin: IrDeclarationOrigin? = null,
    parentOrigin: IrDeclarationOrigin? = null,
    fakeOverrideOwnerLookupTag: ConeClassLikeLookupTag? = null
): IrDeclarationOrigin {
    if (this == null) {
        return predefinedOrigin ?: parentOrigin ?: IrDeclarationOrigin.DEFINED
    }

    val firOrigin = origin
    val computed = when {
        firOrigin is FirDeclarationOrigin.Plugin -> GeneratedByPlugin(firOrigin.key)

        this is FirValueParameter -> when (name) {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR -> IrDeclarationOrigin.UNDERSCORE_PARAMETER
            SpecialNames.DESTRUCT -> IrDeclarationOrigin.DESTRUCTURED_OBJECT_PARAMETER
            else -> null
        }

        this is FirCallableDeclaration -> when {
            fakeOverrideOwnerLookupTag != null && fakeOverrideOwnerLookupTag != containingClassLookupTag() -> IrDeclarationOrigin.FAKE_OVERRIDE
            isSubstitutionOrIntersectionOverride || isHiddenToOvercomeSignatureClash == true -> IrDeclarationOrigin.FAKE_OVERRIDE
            parentOrigin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB && symbol.isJavaOrEnhancement -> {
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
            }
            symbol.origin is FirDeclarationOrigin.Plugin -> GeneratedByPlugin((symbol.origin as FirDeclarationOrigin.Plugin).key)
            else -> null
        }
        else -> null
    }

    return computed ?: predefinedOrigin ?: parentOrigin ?: IrDeclarationOrigin.DEFINED
}

private typealias NameWithElementType = Pair<Name, IElementType>

private val PREFIX_POSTFIX_ORIGIN_MAP: Map<NameWithElementType, IrStatementOrigin> = hashMapOf(
    (OperatorNameConventions.INC to KtNodeTypes.PREFIX_EXPRESSION) to IrStatementOrigin.PREFIX_INCR,
    (OperatorNameConventions.INC to KtNodeTypes.POSTFIX_EXPRESSION) to IrStatementOrigin.POSTFIX_INCR,
    (OperatorNameConventions.DEC to KtNodeTypes.PREFIX_EXPRESSION) to IrStatementOrigin.PREFIX_DECR,
    (OperatorNameConventions.DEC to KtNodeTypes.POSTFIX_EXPRESSION) to IrStatementOrigin.POSTFIX_DECR,
)

fun FirVariableAssignment.getIrAssignmentOrigin(): IrStatementOrigin {
    incOrDeclSourceKindToIrStatementOrigin[source?.kind]?.let { return it }
    augmentedAssignSourceKindToIrStatementOrigin[source?.kind]?.let { return it }
    val callableName = getCallableNameFromIntClassIfAny() ?: return IrStatementOrigin.EQ
    PREFIX_POSTFIX_ORIGIN_MAP[callableName to source?.elementType]?.let { return it }

    val rValue = rValue as FirFunctionCall
    val kind = rValue.source?.kind

    return when (kind) {
        KtFakeSourceElementKind.DesugaredPrefixInc, KtFakeSourceElementKind.DesugaredPostfixInc -> IrStatementOrigin.PLUSEQ
        KtFakeSourceElementKind.DesugaredPrefixDec, KtFakeSourceElementKind.DesugaredPostfixDec -> IrStatementOrigin.MINUSEQ
        else -> IrStatementOrigin.EQ
    }
}

fun FirVariableAssignment.getIrPrefixPostfixOriginIfAny(): IrStatementOrigin? {
    val callableName = getCallableNameFromIntClassIfAny() ?: return null
    return PREFIX_POSTFIX_ORIGIN_MAP[callableName to source?.elementType]
}

private fun FirVariableAssignment.getCallableNameFromIntClassIfAny(): Name? {
    val calleeReferenceSymbol = calleeReference?.toResolvedCallableSymbol() ?: return null
    val rValue = rValue
    if (rValue is FirFunctionCall && calleeReferenceSymbol.callableId.isLocal) {
        val callableId = rValue.calleeReference.toResolvedCallableSymbol()?.callableId
        if (callableId?.classId == StandardClassIds.Int) {
            return callableId.callableName
        }
    }
    return null
}

fun FirCallableDeclaration.contextReceiversForFunctionOrContainingProperty(): List<FirContextReceiver> =
    if (this is FirPropertyAccessor)
        this.propertySymbol.fir.contextReceivers
    else
        this.contextReceivers

fun List<IrDeclaration>.extractFirDeclarations(): Set<FirDeclaration> {
    return this.mapNotNullTo(mutableSetOf()) { ((it as IrMetadataSourceOwner).metadata as FirMetadataSource).fir }
}

// This method is intended to be used for default values of annotation parameters (compile-time strings, numbers, enum values, KClasses)
// where they are needed and may produce incorrect results for values that may be encountered outside annotations.
fun FirExpression.asCompileTimeIrInitializer(components: Fir2IrComponents, expectedType: ConeKotlinType? = null): IrExpressionBody {
    val visitor = Fir2IrVisitor(components, Fir2IrConversionScope(components.configuration))
    val expression = visitor.convertToIrExpression(this, expectedType = expectedType)
    return components.irFactory.createExpressionBody(expression)
}

/**
 * Note: for componentN call, we have to change the type here (to the original component type) to keep compatibility with PSI2IR
 * Some backend optimizations related to withIndex() probably depend on this type: index should always be Int
 * See e.g. forInStringWithIndexWithExplicitlyTypedIndexVariable.kt from codegen box tests
 *
 * [predefinedType] is needed for case, when this function is used to convert some variable access, and
 *   default IR type, for it is already known
 *   It's not correct to always use converted [this.returnTypeRef] in one particular case:
 *
 * val <T> T.some: T
 *     get() = ...
 *     set(value) {
 *         field = value <----
 *     }
 *
 *  Here `value` has type `T`. In FIR there is one type parameter `T` for the whole property
 *  But in IR we have different type parameters for getter and setter. And by default `toIrType(c)` transforms
 *    `T` as type parameter of getter, but here we are in context of the setter. And in CallAndReferenceGenerator.convertToIrCall
 *    we already know that `value` should have type `T[set-some]`, so this type is provided as [predefinedType]
 *
 *  The alternative could be to determine outside that we are in scope of setter and pass type origin, but it's
 *    much more complicated and messy
 */
internal fun FirVariable.irTypeForPotentiallyComponentCall(c: Fir2IrComponents, predefinedType: IrType? = null): IrType {
    val initializer = initializer
    val typeRef = when {
        isVal && initializer is FirComponentCall -> initializer.resolvedType
        else -> {
            if (predefinedType != null) return predefinedType
            this.returnTypeRef.coneType
        }
    }
    return typeRef.toIrType(c)
}

internal val FirValueParameter.varargElementType: ConeKotlinType?
    get() {
        if (!isVararg) return null
        return returnTypeRef.coneType.arrayElementType()
    }

internal fun FirClassSymbol<*>.unsubstitutedScope(c: Fir2IrComponents): FirTypeScope {
    return this.unsubstitutedScope(c.session, c.scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
}

internal fun FirClass.unsubstitutedScope(c: Fir2IrComponents): FirTypeScope {
    return symbol.unsubstitutedScope(c)
}

internal fun FirClassSymbol<*>.declaredScope(c: Fir2IrComponents): FirContainingNamesAwareScope {
    return this.declaredMemberScope(c.session, memberRequiredPhase = null)
}

internal fun FirClass.declaredScope(c: Fir2IrComponents): FirContainingNamesAwareScope {
    return symbol.declaredScope(c)
}

internal fun implicitCast(original: IrExpression, castType: IrType, typeOperator: IrTypeOperator): IrExpression {
    if (original.type == castType) {
        return original
    }
    if (original !is IrTypeOperatorCall) {
        return IrTypeOperatorCallImpl(
            original.startOffset,
            original.endOffset,
            castType,
            typeOperator,
            castType,
            original
        )
    }
    return implicitCast(original.argument, castType, typeOperator)
}

internal fun FirQualifiedAccessExpression.buildSubstitutorByCalledCallable(c: Fir2IrComponents): ConeSubstitutor {
    val typeParameters = when (val declaration = calleeReference.toResolvedCallableSymbol()?.fir) {
        is FirFunction -> declaration.typeParameters
        is FirProperty -> declaration.typeParameters
        else -> return ConeSubstitutor.Empty
    }
    val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
    for ((index, typeParameter) in typeParameters.withIndex()) {
        val typeProjection = typeArguments.getOrNull(index) as? FirTypeProjectionWithVariance ?: continue
        map[typeParameter.symbol] = typeProjection.typeRef.coneType
    }
    return ConeSubstitutorByMap.create(map, c.session)
}

val augmentedAssignSourceKindToIrStatementOrigin = mapOf(
    KtFakeSourceElementKind.DesugaredPlusAssign to IrStatementOrigin.PLUSEQ,
    KtFakeSourceElementKind.DesugaredMinusAssign to IrStatementOrigin.MINUSEQ,
    KtFakeSourceElementKind.DesugaredTimesAssign to IrStatementOrigin.MULTEQ,
    KtFakeSourceElementKind.DesugaredDivAssign to IrStatementOrigin.DIVEQ,
    KtFakeSourceElementKind.DesugaredRemAssign to IrStatementOrigin.PERCEQ
)

val incOrDeclSourceKindToIrStatementOrigin = mapOf(
    KtFakeSourceElementKind.DesugaredPrefixInc to IrStatementOrigin.PREFIX_INCR,
    KtFakeSourceElementKind.DesugaredPostfixInc to IrStatementOrigin.POSTFIX_INCR,
    KtFakeSourceElementKind.DesugaredPrefixDec to IrStatementOrigin.PREFIX_DECR,
    KtFakeSourceElementKind.DesugaredPostfixDec to IrStatementOrigin.POSTFIX_DECR,
    KtFakeSourceElementKind.DesugaredPrefixIncSecondGetReference to IrStatementOrigin.PREFIX_INCR,
    KtFakeSourceElementKind.DesugaredPrefixDecSecondGetReference to IrStatementOrigin.PREFIX_DECR
)

internal inline fun <R> convertCatching(element: FirElement, conversionScope: Fir2IrConversionScope? = null, block: () -> R): R {
    try {
        return block()
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        errorWithAttachment("Exception was thrown during transformation of ${element::class.java}", cause = e) {
            withFirEntry("element", element)
            conversionScope?.containingFileIfAny()?.let { withEntry("file", it.path) }
        }
    }
}
