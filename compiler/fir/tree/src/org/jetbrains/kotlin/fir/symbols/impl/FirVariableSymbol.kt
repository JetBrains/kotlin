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

sealed class FirVariableSymbol<out E : FirVariable> : FirCallableSymbol<E>() {
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

sealed class FirPropertySymbol : FirVariableSymbol<FirProperty>(), PropertySymbolMarker {
    abstract val isLocal: Boolean

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
 * Used for purely local properties, which are declared in a functions
 */
class FirLocalPropertySymbol() : FirPropertySymbol() {
    override val callableId: CallableId?
        get() = null

    override val isLocal: Boolean
        get() = true
}

/**
 * Used for top-level and member properties, including member properties of local classes / anonymous objects
 */
open class FirRegularPropertySymbol(override val callableId: CallableId) : FirPropertySymbol() {
    override val isLocal: Boolean
        get() = false
}

class FirIntersectionOverridePropertySymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirRegularPropertySymbol(callableId), FirIntersectionCallableSymbol

class FirIntersectionOverrideFieldSymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirFieldSymbol(callableId), FirIntersectionCallableSymbol

class FirBackingFieldSymbol : FirVariableSymbol<FirBackingField>() {
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

class FirDelegateFieldSymbol(val correspondingPropertySymbol: FirPropertySymbol) : FirVariableSymbol<FirProperty>() {
    override val callableId: CallableId?
        get() = correspondingPropertySymbol.callableId
}

open class FirFieldSymbol(override val callableId: CallableId) : FirVariableSymbol<FirField>() {
    val hasInitializer: Boolean
        get() = fir.initializer != null

    val hasConstantInitializer: Boolean
        get() = fir.hasConstantInitializer
}

class FirEnumEntrySymbol(override val callableId: CallableId) : FirVariableSymbol<FirEnumEntry>(), EnumEntrySymbolMarker {
    val initializerObjectSymbol: FirAnonymousObjectSymbol?
        get() = (fir.initializer as? FirAnonymousObjectExpression)?.anonymousObject?.symbol
}

class FirValueParameterSymbol() : FirVariableSymbol<FirValueParameter>(),
    ValueParameterSymbolMarker,
    // TODO(KT-72994) stop extending FirThisOwnerSymbol when context receivers are removed
    FirThisOwnerSymbol<FirValueParameter> {
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

// TODO(KT-72994) convert to class extending FirBasedSymbol when context receivers are removed
sealed interface FirThisOwnerSymbol<out E : FirDeclaration> {
    val fir: E
    val source: KtSourceElement?
}

class FirReceiverParameterSymbol : FirBasedSymbol<FirReceiverParameter>(), FirThisOwnerSymbol<FirReceiverParameter> {
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
    val diagnostic: ConeDiagnostic
) : FirPropertySymbol(), FirErrorCallableSymbol<FirProperty> {
    override val callableId: CallableId
        get() = CALLABLE_ID

    override val isLocal: Boolean
        get() = false

    companion object {
        val NAME: Name = Name.special("<error property>")
        val CALLABLE_ID: CallableId = CallableId(FqName.ROOT, NAME)
    }
}
