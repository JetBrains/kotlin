/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.mpp.EnumEntrySymbolMarker
import org.jetbrains.kotlin.mpp.PropertySymbolMarker
import org.jetbrains.kotlin.mpp.ValueParameterSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirVariableSymbol<out E : FirVariable>(
    symbolId: FirSymbolId<FirVariableSymbol<E>>,
) : FirCallableSymbol<E>(symbolId) {
    abstract override val symbolId: FirSymbolId<FirVariableSymbol<E>>

    val resolvedInitializer: FirExpression?
        get() {
            if (fir.initializer == null) return null
            val requiredPhase = when {
                this.isConst -> FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
                else -> FirResolvePhase.BODY_RESOLVE
            }
            lazyResolveToPhase(requiredPhase)
            return fir.initializer
        }

    val resolvedDefaultValue: FirExpression?
        get() {
            val valueParameter = fir as? FirValueParameter
            if (valueParameter?.defaultValue == null) return null

            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return valueParameter.defaultValue
        }

    val isVal: Boolean
        get() = fir.isVal

    val isVar: Boolean
        get() = fir.isVar

    override val name: Name
        get() = fir.name
}

sealed class FirPropertySymbol(
    symbolId: FirSymbolId<FirPropertySymbol>,
) : FirVariableSymbol<FirProperty>(symbolId), PropertySymbolMarker {
    abstract override val symbolId: FirSymbolId<FirPropertySymbol>

    open val getterSymbol: FirPropertyAccessorSymbol?
        get() = fir.getter?.symbol

    open val setterSymbol: FirPropertyAccessorSymbol?
        get() = fir.setter?.symbol

    val backingFieldSymbol: FirBackingFieldSymbol?
        get() = fir.backingField?.symbol

    val delegateFieldSymbol: FirDelegateFieldSymbol?
        get() = fir.delegateFieldSymbol

    val delegate: FirExpression?
        get() = fir.delegate

    val hasDelegate: Boolean
        get() = fir.delegate != null

    val hasInitializer: Boolean
        get() = fir.initializer != null

    val initializerSource: KtSourceElement?
        get() = fir.initializer?.source

    val controlFlowGraphReference: FirControlFlowGraphReference?
        get() {
            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return fir.controlFlowGraphReference
        }

    override fun deprecationsAreDefinitelyEmpty(): Boolean {
        return currentDeclarationDeprecationsAreDefinitelyEmpty()
                && getterSymbol?.currentDeclarationDeprecationsAreDefinitelyEmpty() != false
                && setterSymbol?.currentDeclarationDeprecationsAreDefinitelyEmpty() != false
    }
}

/**
 * Used for purely local properties, which are declared in a function.
 */
class FirLocalPropertySymbol(
    override val symbolId: FirSymbolId<FirLocalPropertySymbol>,
) : FirPropertySymbol(symbolId) {
    constructor() : this(FirUniqueSymbolId())

    override val callableId: CallableId?
        get() = null
}

/**
 * Used for top-level and member properties, including member properties of local classes / anonymous objects
 */
open class FirRegularPropertySymbol(
    override val symbolId: FirSymbolId<FirRegularPropertySymbol>,
    override val callableId: CallableId,
) : FirPropertySymbol(symbolId) {
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)
}

class FirIntersectionOverridePropertySymbol(
    override val symbolId: FirSymbolId<FirIntersectionOverridePropertySymbol>,
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirRegularPropertySymbol(symbolId, callableId), FirIntersectionCallableSymbol

class FirIntersectionOverrideFieldSymbol(
    override val symbolId: FirSymbolId<FirIntersectionOverrideFieldSymbol>,
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirFieldSymbol(symbolId, callableId), FirIntersectionCallableSymbol

class FirBackingFieldSymbol(
    override val symbolId: FirSymbolId<FirBackingFieldSymbol>,
) : FirVariableSymbol<FirBackingField>(symbolId) {
    constructor() : this(FirUniqueSymbolId())

    override val callableId: CallableId
        get() = CallableId(name)

    val propertySymbol: FirPropertySymbol
        get() = fir.propertySymbol

    val getterSymbol: FirPropertyAccessorSymbol?
        get() = fir.propertySymbol.fir.getter?.symbol

    override fun deprecationsAreDefinitelyEmpty(): Boolean {
        return currentDeclarationDeprecationsAreDefinitelyEmpty() &&
                propertySymbol.currentDeclarationDeprecationsAreDefinitelyEmpty()
    }
}

class FirDelegateFieldSymbol(
    override val symbolId: FirSymbolId<FirDelegateFieldSymbol>,
    val correspondingPropertySymbol: FirPropertySymbol,
) : FirVariableSymbol<FirProperty>(symbolId) {
    constructor(correspondingPropertySymbol: FirPropertySymbol) : this(FirUniqueSymbolId(), correspondingPropertySymbol)

    override val callableId: CallableId?
        get() = correspondingPropertySymbol.callableId
}

open class FirFieldSymbol(
    override val symbolId: FirSymbolId<FirFieldSymbol>,
    override val callableId: CallableId,
) : FirVariableSymbol<FirField>(symbolId) {
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val hasInitializer: Boolean
        get() = fir.initializer != null

    val hasConstantInitializer: Boolean
        get() = fir.hasConstantInitializer
}

class FirEnumEntrySymbol(
    override val symbolId: FirSymbolId<FirEnumEntrySymbol>,
    override val callableId: CallableId,
) : FirVariableSymbol<FirEnumEntry>(symbolId), EnumEntrySymbolMarker {
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val initializerObjectSymbol: FirAnonymousObjectSymbol?
        get() = (fir.initializer as? FirAnonymousObjectExpression)?.anonymousObject?.symbol
}

class FirValueParameterSymbol(
    override val symbolId: FirSymbolId<FirValueParameterSymbol>,
) : FirVariableSymbol<FirValueParameter>(symbolId), ValueParameterSymbolMarker {
    constructor() : this(FirUniqueSymbolId())

    override val callableId: CallableId
        get() = CallableId(name)

    val hasDefaultValue: Boolean
        get() = fir.defaultValue != null

    val defaultValueSource: KtSourceElement?
        get() = fir.defaultValue?.source

    val isCrossinline: Boolean
        get() = fir.isCrossinline

    val isNoinline: Boolean
        get() = fir.isNoinline

    val isVararg: Boolean
        get() = fir.isVararg

    val containingDeclarationSymbol: FirBasedSymbol<*>
        get() = fir.containingDeclarationSymbol
}

sealed class FirThisOwnerSymbol<out E : FirDeclaration>(symbolId: FirSymbolId<FirThisOwnerSymbol<E>>) : FirBasedSymbol<E>(symbolId)

class FirReceiverParameterSymbol(
    override val symbolId: FirSymbolId<FirReceiverParameterSymbol>,
) : FirThisOwnerSymbol<FirReceiverParameter>(symbolId) {
    /**
     * Creates a [FirReceiverParameterSymbol] with a *unique* symbol ID ([FirUniqueSymbolId]). This constructor should only be used for
     * symbols which are stored for the lifetime of the session. In particular, type alias symbols built from light tree/PSI should not use
     * this constructor. See [FirSymbolId] for more information.
     */
    constructor() : this(FirUniqueSymbolId())

    val containingDeclarationSymbol: FirBasedSymbol<*>
        get() = fir.containingDeclarationSymbol

    val resolvedType: ConeKotlinType
        get() = calculateResolvedTypeRef().coneType

    private fun receiverTypeRef(): FirTypeRef {
        return fir.typeRef
    }

    fun calculateResolvedTypeRef(): FirResolvedTypeRef {
        val receiverTypeRef = receiverTypeRef()
        if (receiverTypeRef is FirResolvedTypeRef) {
            return receiverTypeRef
        }
        lazyResolveToPhase(FirResolvePhase.TYPES)
        val result = receiverTypeRef()
        if (result !is FirResolvedTypeRef) {
            errorInLazyResolve("receiverTypeRef", receiverTypeRef::class, FirResolvedTypeRef::class)
        }
        return result
    }

    override fun toString(): String = "FirReceiverParameterSymbol"
}

// TODO (marco): Same as with `FirErrorFunctionSymbol`: Do we default to unique and call it a day?
class FirErrorPropertySymbol(
    override val symbolId: FirSymbolId<FirErrorPropertySymbol>,
    val diagnostic: ConeDiagnostic,
) : FirRegularPropertySymbol(CALLABLE_ID), FirErrorCallableSymbol<FirProperty> {
    constructor(diagnostic: ConeDiagnostic) : this(FirUniqueSymbolId(), diagnostic)

    companion object {
        val NAME: Name = Name.special("<error property>")
        val CALLABLE_ID: CallableId = CallableId(FqName.ROOT, NAME)
    }
}
