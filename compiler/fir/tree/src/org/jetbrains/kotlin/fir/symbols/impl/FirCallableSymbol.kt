/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

abstract class FirCallableSymbol<D : FirCallableDeclaration> : FirBasedSymbol<D>() {
    abstract val callableId: CallableId

    val resolvedReturnTypeRef: FirResolvedTypeRef
        get() {
            ensureType(fir.returnTypeRef)
            return fir.returnTypeRef as FirResolvedTypeRef
        }

    val resolvedReceiverTypeRef: FirResolvedTypeRef?
        get() {
            ensureType(fir.receiverTypeRef)
            return fir.receiverTypeRef as FirResolvedTypeRef?
        }

    val resolvedStatus: FirResolvedDeclarationStatus
        get() {
            ensureResolved(FirResolvePhase.STATUS)
            return fir.status as FirResolvedDeclarationStatus
        }

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() {
            return fir.typeParameters.map { it.symbol }
        }

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType

    val name: Name
        get() = callableId.callableName

    val deprecation: DeprecationsPerUseSite?
        get() {
            ensureResolved(FirResolvePhase.STATUS)
            return fir.deprecation
        }

    private fun ensureType(typeRef: FirTypeRef?) {
        when (typeRef) {
            null, is FirResolvedTypeRef -> {}
            is FirImplicitTypeRef -> ensureResolved(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            else -> ensureResolved(FirResolvePhase.TYPES)
        }
    }

    override fun toString(): String = "${this::class.simpleName} $callableId"
}

val FirCallableSymbol<*>.isStatic: Boolean get() = (fir as? FirMemberDeclaration)?.status?.isStatic == true

val FirCallableSymbol<*>.isExtension: Boolean
    get() = when (fir) {
        is FirFunction -> fir.receiverTypeRef != null
        is FirProperty -> fir.receiverTypeRef != null
        is FirVariable -> false
    }
