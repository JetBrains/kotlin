/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.mpp.PropertySymbolMarker
import org.jetbrains.kotlin.mpp.ValueParameterSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirVariableSymbol<E : FirVariable>(override val callableId: CallableId) : FirCallableSymbol<E>() {
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
}

open class FirPropertySymbol(callableId: CallableId, ) : FirVariableSymbol<FirProperty>(callableId), PropertySymbolMarker {
    // TODO: should we use this constructor for local variables?
    constructor(name: Name) : this(CallableId(name))

    val isLocal: Boolean
        get() = fir.isLocal

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

    val isVal: Boolean
        get() = fir.isVal

    val isVar: Boolean
        get() = fir.isVar
}

class FirIntersectionOverridePropertySymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirPropertySymbol(callableId), FirIntersectionCallableSymbol

class FirIntersectionOverrideFieldSymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
    override val containsMultipleNonSubsumed: Boolean,
) : FirFieldSymbol(callableId), FirIntersectionCallableSymbol

class FirBackingFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirBackingField>(callableId) {
    val isVal: Boolean
        get() = fir.isVal

    val isVar: Boolean
        get() = fir.isVar

    val propertySymbol: FirPropertySymbol
        get() = fir.propertySymbol

    val getterSymbol: FirPropertyAccessorSymbol?
        get() = fir.propertySymbol.fir.getter?.symbol
}

class FirDelegateFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirProperty>(callableId)

open class FirFieldSymbol(callableId: CallableId) : FirVariableSymbol<FirField>(callableId) {
    val hasInitializer: Boolean
        get() = fir.initializer != null

    val hasConstantInitializer: Boolean
        get() = fir.hasConstantInitializer

    val isVal: Boolean
        get() = fir.isVal

    val isVar: Boolean
        get() = fir.isVar
}

class FirEnumEntrySymbol(callableId: CallableId) : FirVariableSymbol<FirEnumEntry>(callableId) {
    val initializerObjectSymbol: FirAnonymousObjectSymbol?
        get() = (fir.initializer as? FirAnonymousObjectExpression)?.anonymousObject?.symbol
}

class FirValueParameterSymbol(name: Name) : FirVariableSymbol<FirValueParameter>(CallableId(name)), ValueParameterSymbolMarker {
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

    val containingFunctionSymbol: FirFunctionSymbol<*>
        get() = fir.containingFunctionSymbol
}

class FirErrorPropertySymbol(
    val diagnostic: ConeDiagnostic
) : FirVariableSymbol<FirErrorProperty>(CallableId(FqName.ROOT, NAME)) {
    companion object {
        val NAME: Name = Name.special("<error property>")
    }
}
