/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirFunctionSymbol<D : FirFunction>(
    override val callableId: CallableId
) : FirCallableSymbol<D>() {
    val valueParameterSymbols: List<FirValueParameterSymbol>
        get() = fir.valueParameters.map { it.symbol }

    val resolvedContractDescription: FirResolvedContractDescription?
        get() {
            ensureResolved(FirResolvePhase.CONTRACTS)
            return when (this) {
                is FirNamedFunctionSymbol -> fir.contractDescription
                is FirPropertyAccessorSymbol -> fir.contractDescription
                else -> null
            } as? FirResolvedContractDescription
        }

    val resolvedControlFlowGraphReference: FirControlFlowGraphReference?
        get() {
            ensureResolved(FirResolvePhase.BODY_RESOLVE)
            return fir.controlFlowGraphReference
        }
}

// ------------------------ named ------------------------

open class FirNamedFunctionSymbol(
    callableId: CallableId,
) : FirFunctionSymbol<FirSimpleFunction>(callableId)

interface FirIntersectionCallableSymbol {
    val intersections: Collection<FirCallableSymbol<*>>
}

class FirIntersectionOverrideFunctionSymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>
) : FirNamedFunctionSymbol(callableId), FirIntersectionCallableSymbol

class FirConstructorSymbol(
    callableId: CallableId
) : FirFunctionSymbol<FirConstructor>(callableId) {
    val isPrimary: Boolean
        get() = fir.isPrimary

    val resolvedDelegatedConstructor: FirConstructorSymbol?
        get() {
            val delegatedConstructorCall = resolvedDelegatedConstructorCall ?: return null
            return (delegatedConstructorCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirConstructorSymbol
        }

    val resolvedDelegatedConstructorCall: FirDelegatedConstructorCall?
        get() {
            if (fir.delegatedConstructor == null) return null
            ensureResolved(FirResolvePhase.BODY_RESOLVE)
            return fir.delegatedConstructor
        }

    val delegatedConstructorCallIsThis: Boolean
        get() = fir.delegatedConstructor?.isThis ?: false

    val delegatedConstructorCallIsSuper: Boolean
        get() = fir.delegatedConstructor?.isSuper ?: false
}

open class FirAccessorSymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirPropertySymbol(callableId), AccessorSymbol

// ------------------------ unnamed ------------------------

sealed class FirFunctionWithoutNameSymbol<F : FirFunction>(
    stubName: Name
) : FirFunctionSymbol<F>(CallableId(FqName("special"), stubName))

class FirAnonymousFunctionSymbol : FirFunctionWithoutNameSymbol<FirAnonymousFunction>(Name.identifier("anonymous")) {
    val label: FirLabel? get() = fir.label
}

class FirPropertyAccessorSymbol : FirFunctionWithoutNameSymbol<FirPropertyAccessor>(Name.identifier("accessor"))

class FirPropertyFieldDeclarationSymbol() : FirBasedSymbol<FirBackingField>()

class FirErrorFunctionSymbol : FirFunctionWithoutNameSymbol<FirErrorFunction>(Name.identifier("error"))
