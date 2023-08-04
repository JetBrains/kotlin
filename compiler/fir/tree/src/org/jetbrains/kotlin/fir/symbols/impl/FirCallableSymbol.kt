/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

abstract class FirCallableSymbol<D : FirCallableDeclaration> : FirBasedSymbol<D>(), CallableSymbolMarker {
    abstract val callableId: CallableId

    val resolvedReturnTypeRef: FirResolvedTypeRef
        get() {
            calculateReturnType()
            return fir.returnTypeRef as FirResolvedTypeRef
        }

    fun calculateReturnType() {
        ensureType(fir.returnTypeRef)
        val returnTypeRef = fir.returnTypeRef
        if (returnTypeRef !is FirResolvedTypeRef) {
            errorInLazyResolve("returnTypeRef", returnTypeRef::class, FirResolvedTypeRef::class)
        }
    }

    val resolvedReturnType: ConeKotlinType
        get() = resolvedReturnTypeRef.coneType

    val resolvedReceiverTypeRef: FirResolvedTypeRef?
        get() = calculateReceiverTypeRef()

    private fun calculateReceiverTypeRef(): FirResolvedTypeRef? {
        val receiverParameter = fir.receiverParameter ?: return null
        ensureType(receiverParameter.typeRef)
        val receiverTypeRef = receiverParameter.typeRef
        if (receiverTypeRef !is FirResolvedTypeRef) {
            errorInLazyResolve("receiverTypeRef", receiverTypeRef::class, FirResolvedTypeRef::class)
        }

        return receiverTypeRef
    }

    val receiverParameter: FirReceiverParameter?
        get() {
            calculateReceiverTypeRef()
            return fir.receiverParameter
        }

    val resolvedContextReceivers: List<FirContextReceiver>
        get() {
            if (fir.contextReceivers.isEmpty()) return emptyList()
            lazyResolveToPhase(FirResolvePhase.TYPES)
            return fir.contextReceivers
        }

    val resolvedStatus: FirResolvedDeclarationStatus
        get() = fir.resolvedStatus()

    val rawStatus: FirDeclarationStatus
        get() = fir.status

    val typeParameterSymbols: List<FirTypeParameterSymbol>
        get() = fir.typeParameters.map { it.symbol }

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType

    val name: Name
        get() = callableId.callableName

    fun getDeprecation(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        lazyResolveToPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        return fir.deprecationsProvider.getDeprecationsInfo(languageVersionSettings)
    }

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
