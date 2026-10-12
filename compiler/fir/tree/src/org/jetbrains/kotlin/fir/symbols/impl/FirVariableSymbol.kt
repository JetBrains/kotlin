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
    symbolId: FirSymbolId<FirLocalPropertySymbol>,
) : FirPropertySymbol(symbolId) {
    /**
     * Creates a [FirLocalPropertySymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    override val callableId: CallableId?
        get() = null
}

/**
 * Used for top-level and member properties, including member properties of local classes / anonymous objects
 */
open class FirRegularPropertySymbol(
    symbolId: FirSymbolId<FirRegularPropertySymbol>,
    override val callableId: CallableId,
) : FirPropertySymbol(symbolId) {
    /**
     * Creates a [FirRegularPropertySymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)
}

class FirIntersectionOverridePropertySymbol(
    symbolId: FirSymbolId<FirIntersectionOverridePropertySymbol>,
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirRegularPropertySymbol(symbolId, callableId), FirIntersectionCallableSymbol {
    /**
     * Creates a [FirIntersectionOverridePropertySymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(
        callableId: CallableId,
        intersections: Collection<FirCallableSymbol<*>>,
        containsMultipleNonSubsumed: Boolean,
    ) : this(FirUniqueSymbolId(), callableId, intersections, containsMultipleNonSubsumed)
}

class FirIntersectionOverrideFieldSymbol(
    symbolId: FirSymbolId<FirIntersectionOverrideFieldSymbol>,
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirFieldSymbol(symbolId, callableId), FirIntersectionCallableSymbol {
    /**
     * Creates a [FirIntersectionOverrideFieldSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(
        callableId: CallableId,
        intersections: Collection<FirCallableSymbol<*>>,
        containsMultipleNonSubsumed: Boolean,
    ) : this(FirUniqueSymbolId(), callableId, intersections, containsMultipleNonSubsumed)
}

class FirBackingFieldSymbol(
    symbolId: FirSymbolId<FirBackingFieldSymbol>,
) : FirVariableSymbol<FirBackingField>(symbolId) {
    /**
     * Creates a [FirBackingFieldSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
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
    symbolId: FirSymbolId<FirDelegateFieldSymbol>,
    val correspondingPropertySymbol: FirPropertySymbol,
) : FirVariableSymbol<FirProperty>(symbolId) {
    /**
     * Creates a [FirDelegateFieldSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(correspondingPropertySymbol: FirPropertySymbol) : this(FirUniqueSymbolId(), correspondingPropertySymbol)

    override val callableId: CallableId?
        get() = correspondingPropertySymbol.callableId
}

open class FirFieldSymbol(
    symbolId: FirSymbolId<FirFieldSymbol>,
    override val callableId: CallableId,
) : FirVariableSymbol<FirField>(symbolId) {
    /**
     * Creates a [FirFieldSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val hasInitializer: Boolean
        get() = fir.initializer != null

    val hasConstantInitializer: Boolean
        get() = fir.hasConstantInitializer
}

class FirEnumEntrySymbol(
    symbolId: FirSymbolId<FirEnumEntrySymbol>,
    override val callableId: CallableId,
) : FirVariableSymbol<FirEnumEntry>(symbolId), EnumEntrySymbolMarker {
    /**
     * Creates a [FirEnumEntrySymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(callableId: CallableId) : this(FirUniqueSymbolId(), callableId)

    val initializerObjectSymbol: FirAnonymousObjectSymbol?
        get() = (fir.initializer as? FirAnonymousObjectExpression)?.anonymousObject?.symbol
}

class FirValueParameterSymbol(
    symbolId: FirSymbolId<FirValueParameterSymbol>,
) : FirVariableSymbol<FirValueParameter>(symbolId), ValueParameterSymbolMarker {
    /**
     * Creates a [FirValueParameterSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    override val callableId: CallableId
        get() = CallableId(name)

    val hasDefaultValue: Boolean
        get() = fir.defaultValue != null

    val resolvedDefaultValueSource: KtSourceElement?
        get() = resolvedDefaultValue?.source

    val resolvedDefaultValue: FirExpression?
        get() {
            if (fir.defaultValue == null) return null

            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return fir.defaultValue
        }

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
    symbolId: FirSymbolId<FirReceiverParameterSymbol>,
) : FirThisOwnerSymbol<FirReceiverParameter>(symbolId) {
    /**
     * Creates a [FirReceiverParameterSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
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

class FirErrorPropertySymbol(
    symbolId: FirSymbolId<FirErrorPropertySymbol>,
    val diagnostic: ConeDiagnostic,
) : FirRegularPropertySymbol(symbolId, CALLABLE_ID), FirErrorCallableSymbol<FirProperty> {
    /**
     * Creates a [FirErrorPropertySymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(diagnostic: ConeDiagnostic) : this(FirUniqueSymbolId(), diagnostic)

    companion object {
        val NAME: Name = Name.special("<error property>")
        val CALLABLE_ID: CallableId = CallableId(FqName.ROOT, NAME)
    }
}
