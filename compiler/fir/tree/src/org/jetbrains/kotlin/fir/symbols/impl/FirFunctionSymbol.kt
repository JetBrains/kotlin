/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.contracts.FirErrorContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.mpp.ConstructorSymbolMarker
import org.jetbrains.kotlin.mpp.FunctionSymbolMarker
import org.jetbrains.kotlin.mpp.SimpleFunctionSymbolMarker
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

sealed class FirFunctionSymbol<out D : FirFunction>(
    symbolId: FirSymbolId<FirFunctionSymbol<D>>,
    override val callableId: CallableId,
) : FirCallableSymbol<D>(symbolId), FunctionSymbolMarker {
    override val name: Name
        get() = callableId.callableName

    val valueParameterSymbols: List<FirValueParameterSymbol>
        get() = fir.valueParameters.map { it.symbol }

    val resolvedContractDescription: FirResolvedContractDescription?
        get() {
            calculateContractDescription()
            return (fir as? FirContractDescriptionOwner)?.contractDescription as? FirResolvedContractDescription?
        }

    fun calculateContractDescription() {
        val fir = fir as? FirContractDescriptionOwner ?: return
        when (fir.contractDescription) {
            null, is FirErrorContractDescription, is FirResolvedContractDescription -> {}
            is FirLegacyRawContractDescription, is FirRawContractDescription -> {
                lazyResolveToPhase(FirResolvePhase.CONTRACTS)
                when (val description = fir.contractDescription) {
                    null, is FirErrorContractDescription, is FirResolvedContractDescription -> {}
                    is FirLegacyRawContractDescription, is FirRawContractDescription -> {
                        errorWithAttachment("Expected a resolved contract description, but got: ${description::class.simpleName}") {
                            withFirEntry("fir", fir)
                        }
                    }
                }
            }
        }
    }

    val hasBody: Boolean
        get() = fir.body != null

    val bodySource: KtSourceElement?
        get() = fir.body?.source
}

// ------------------------ named ------------------------

open class FirNamedFunctionSymbol(
    symbolId: FirSymbolId<FirNamedFunctionSymbol>,
    callableId: CallableId,
) : FirFunctionSymbol<FirNamedFunction>(symbolId, callableId), SimpleFunctionSymbolMarker {
    /**
     * Creates a [FirNamedFunctionSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val isSynthetic: Boolean get() = fir.isSynthetic
}

interface FirIntersectionCallableSymbol {
    val intersections: Collection<FirCallableSymbol<*>>

    /**
     * `true` iff a call to `nonSubsumed()` for `intersections` would result into a list with more than one symbol.
     * Intuitively, `false` means this intersection is, strictly speaking, redundant, but we still created it
     * as an implementation detail.
     */
    val containsMultipleNonSubsumed: Boolean
}

class FirIntersectionOverrideFunctionSymbol(
    symbolId: FirSymbolId<FirIntersectionOverrideFunctionSymbol>,
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirNamedFunctionSymbol(symbolId, callableId), FirIntersectionCallableSymbol {
    /**
     * Creates a [FirIntersectionOverrideFunctionSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(
        callableId: CallableId,
        intersections: Collection<FirCallableSymbol<*>>,
        containsMultipleNonSubsumed: Boolean,
    ) : this(FirUniqueSymbolId(), callableId, intersections, containsMultipleNonSubsumed)
}

class FirConstructorSymbol(
    symbolId: FirSymbolId<FirConstructorSymbol>,
    callableId: CallableId,
) : FirFunctionSymbol<FirConstructor>(symbolId, callableId), ConstructorSymbolMarker {
    constructor(symbolId: FirSymbolId<FirConstructorSymbol>, classId: ClassId) : this(symbolId, classId.callableIdForConstructor())

    /**
     * Creates a [FirConstructorSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(classId: ClassId) : this(FirUniqueSymbolId(), classId)

    /**
     * Creates a [FirConstructorSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val isPrimary: Boolean
        get() = fir.isPrimary

    val resolvedDelegatedConstructor: FirConstructorSymbol?
        get() = resolvedDelegatedConstructorCall?.calleeReference?.toResolvedConstructorSymbol()

    val resolvedDelegatedConstructorCall: FirDelegatedConstructorCall?
        get() {
            if (fir.delegatedConstructor == null) return null
            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return fir.delegatedConstructor
        }

    val delegatedConstructorCallIsThis: Boolean
        get() = fir.delegatedConstructor?.isThis == true

}

/**
 * This is a property symbol which is always bound to FirSyntheticProperty.
 *
 * Synthetic property symbol is effectively a combination of
 * a property (which never exists in sources) and
 * a getter which exists in sources and is either from Java or overrides another getter from Java.
 */
abstract class FirSyntheticPropertySymbol(
    symbolId: FirSymbolId<FirSyntheticPropertySymbol>,
    propertyId: CallableId,
    val getterId: CallableId,
) : FirRegularPropertySymbol(symbolId, propertyId) {
    @SymbolInternals
    val syntheticProperty: FirSyntheticProperty
        get() = fir as FirSyntheticProperty

    override val getterSymbol: FirSyntheticPropertyAccessorSymbol?
        get() = super.getterSymbol as FirSyntheticPropertyAccessorSymbol?

    override val setterSymbol: FirSyntheticPropertyAccessorSymbol?
        get() = super.setterSymbol as FirSyntheticPropertyAccessorSymbol?
}

// ------------------------ unnamed ------------------------

sealed class FirFunctionWithoutNameSymbol<out F : FirFunction>(
    symbolId: FirSymbolId<FirFunctionWithoutNameSymbol<F>>,
    stubName: Name,
) : FirFunctionSymbol<F>(symbolId, CallableId(FqName("special"), stubName))

class FirAnonymousFunctionSymbol(
    symbolId: FirSymbolId<FirAnonymousFunctionSymbol>,
) : FirFunctionWithoutNameSymbol<FirAnonymousFunction>(symbolId, Name.identifier("anonymous")) {
    /**
     * Creates a [FirAnonymousFunctionSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    val label: FirLabel? get() = fir.label
    val isLambda: Boolean get() = fir.isLambda
    val inlineStatus: InlineStatus get() = fir.inlineStatus
    val invocationKind: EventOccurrencesRange? get() = fir.invocationKind
    val resolvedTypeRef: FirResolvedTypeRef
        get() {
            val typeRef = fir.typeRef
            checkWithAttachment(
                typeRef is FirResolvedTypeRef,
                message = { "Type of the lambda is not resolved" }
            ) {
                withFirSymbolEntry("lambda", this@FirAnonymousFunctionSymbol)
            }
            return typeRef
        }
}

open class FirPropertyAccessorSymbol(
    symbolId: FirSymbolId<FirPropertyAccessorSymbol>,
) : FirFunctionWithoutNameSymbol<FirPropertyAccessor>(symbolId, Name.identifier("accessor")) {
    /**
     * Creates a [FirPropertyAccessorSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    val isGetter: Boolean get() = fir.isGetter
    val isSetter: Boolean get() = fir.isSetter
    val isDefault: Boolean get() = fir is FirDefaultPropertyAccessor
    open val propertySymbol: FirPropertySymbol get() = fir.propertySymbol
}

class FirSyntheticPropertyAccessorSymbol(
    symbolId: FirSymbolId<FirSyntheticPropertyAccessorSymbol>,
) : FirPropertyAccessorSymbol(symbolId) {
    /**
     * Creates a [FirSyntheticPropertyAccessorSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    override val propertySymbol: FirSyntheticPropertySymbol
        get() = super.propertySymbol as FirSyntheticPropertySymbol

    val delegateFunctionSymbol: FirNamedFunctionSymbol
        get() = (fir as FirSyntheticPropertyAccessor).delegate.symbol
}

interface FirErrorCallableSymbol<F : FirCallableDeclaration>

class FirErrorFunctionSymbol(
    symbolId: FirSymbolId<FirErrorFunctionSymbol>,
) : FirFunctionWithoutNameSymbol<FirErrorFunction>(symbolId, Name.identifier("error")),
    FirErrorCallableSymbol<FirErrorFunction> {
    /**
     * Creates a [FirErrorFunctionSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())
}
