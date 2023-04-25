/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
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

    val resolvedReturnType: ConeKotlinType
        get() = resolvedReturnTypeRef.coneType


    val resolvedReceiverTypeRef: FirResolvedTypeRef?
        get() {
            ensureType(fir.receiverParameter?.typeRef)
            return fir.receiverParameter?.typeRef as FirResolvedTypeRef?
        }

    val receiverParameter: FirReceiverParameter?
        get() {
            ensureType(fir.receiverParameter?.typeRef)
            return fir.receiverParameter
        }

    val resolvedContextReceivers: List<FirContextReceiver>
        get() {
            if (fir.contextReceivers.isEmpty()) return emptyList()
            lazyResolveToPhase(FirResolvePhase.TYPES)
            return fir.contextReceivers
        }

    val resolvedStatus: FirResolvedDeclarationStatus
        get() {
            lazyResolveToPhase(FirResolvePhase.STATUS)
            return fir.status as FirResolvedDeclarationStatus
        }

    val rawStatus: FirDeclarationStatus
        get() = fir.status


    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() {
            return fir.typeParameters.map { it.symbol }
        }

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType

    val name: Name
        get() = callableId.callableName

    fun getDeprecation(apiVersion: ApiVersion): DeprecationsPerUseSite? {
        if (!canBeDeprecated()) return null

        lazyResolveToPhase(FirResolvePhase.TYPES)
        return fir.deprecationsProvider.getDeprecationsInfo(apiVersion)
    }

    /**
     * Checks if symbol can be deprecated by syntax
     * @return `true` if symbol might have some deprecation status, or `false` if it's definitely not deprecated
     */
    internal open fun canBeDeprecated(): Boolean = true

    private fun ensureType(typeRef: FirTypeRef?) {
        when (typeRef) {
            null, is FirResolvedTypeRef -> {}
            is FirImplicitTypeRef -> lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            else -> lazyResolveToPhase(FirResolvePhase.TYPES)
        }
    }

    override fun toString(): String = "${this::class.simpleName} $callableId"
}

val FirCallableSymbol<*>.isExtension: Boolean
    get() = when (fir) {
        is FirFunction -> fir.receiverParameter != null
        is FirProperty -> fir.receiverParameter != null
        is FirVariable -> false
    }
