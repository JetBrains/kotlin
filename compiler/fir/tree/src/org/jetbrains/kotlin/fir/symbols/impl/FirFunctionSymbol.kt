/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.mpp.ConstructorSymbolMarker
import org.jetbrains.kotlin.mpp.FunctionSymbolMarker
import org.jetbrains.kotlin.mpp.SimpleFunctionSymbolMarker
import org.jetbrains.kotlin.name.*

sealed class FirFunctionSymbol<D : FirFunction>(override val callableId: CallableId) : FirCallableSymbol<D>(), FunctionSymbolMarker {
    val valueParameterSymbols: List<FirValueParameterSymbol>
        get() = fir.valueParameters.map { it.symbol }

    val resolvedContractDescription: FirResolvedContractDescription?
        get() {
            lazyResolveToPhase(FirResolvePhase.CONTRACTS)
            return when (this) {
                is FirNamedFunctionSymbol -> fir.contractDescription
                is FirPropertyAccessorSymbol -> fir.contractDescription
                else -> null
            } as? FirResolvedContractDescription
        }

    val resolvedControlFlowGraphReference: FirControlFlowGraphReference?
        get() {
            lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return fir.controlFlowGraphReference
        }
}

// ------------------------ named ------------------------

open class FirNamedFunctionSymbol(callableId: CallableId) : FirFunctionSymbol<FirSimpleFunction>(callableId), SimpleFunctionSymbolMarker

interface FirIntersectionCallableSymbol {
    val intersections: Collection<FirCallableSymbol<*>>
}

class FirIntersectionOverrideFunctionSymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>,
) : FirNamedFunctionSymbol(callableId), FirIntersectionCallableSymbol

class FirConstructorSymbol(callableId: CallableId) : FirFunctionSymbol<FirConstructor>(callableId), ConstructorSymbolMarker {
    constructor(classId: ClassId) : this(classId.callableIdForConstructor())

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
abstract class FirSyntheticPropertySymbol(propertyId: CallableId, val getterId: CallableId) : FirPropertySymbol(propertyId) {
    abstract fun copy(): FirSyntheticPropertySymbol
}

// ------------------------ unnamed ------------------------

sealed class FirFunctionWithoutNameSymbol<F : FirFunction>(stubName: Name) : FirFunctionSymbol<F>(CallableId(FqName("special"), stubName))

class FirAnonymousFunctionSymbol : FirFunctionWithoutNameSymbol<FirAnonymousFunction>(Name.identifier("anonymous")) {
    val label: FirLabel? get() = fir.label
}

class FirPropertyAccessorSymbol : FirFunctionWithoutNameSymbol<FirPropertyAccessor>(Name.identifier("accessor")) {
    val isGetter: Boolean get() = fir.isGetter
    val isSetter: Boolean get() = fir.isSetter
    val propertySymbol get() = fir.propertySymbol
}

class FirErrorFunctionSymbol : FirFunctionWithoutNameSymbol<FirErrorFunction>(Name.identifier("error"))
