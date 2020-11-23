/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

fun AbstractFirBasedSymbol<*>.ensureResolved(
    requiredPhase: FirResolvePhase,
    // TODO: Currently, the parameter is unused but it's needed to guarantee that all call-sites are able to supply use-site session
    // TODO: Decide which one session should be used and probably get rid of the parameter if use-site session is not needed
    @Suppress("UNUSED_PARAMETER") useSiteSession: FirSession,
) {
    val fir = fir as FirDeclaration
    val availablePhase = fir.resolvePhase
    if (availablePhase >= requiredPhase) return
    val resolver = fir.session.phaseManager
        ?: error("phaseManager should be defined when working with FIR in phased mode")

    resolver.ensureResolved(this, requiredPhase)
}

fun AbstractFirBasedSymbol<*>.ensureResolvedForCalls(
    useSiteSession: FirSession,
) {
    val fir = fir as FirDeclaration
    if (fir.resolvePhase >= FirResolvePhase.DECLARATIONS) return

//    val requiredPhase = when (fir) {
//        is FirFunction<*>, is FirProperty -> FirResolvePhase.CONTRACTS
//        else -> FirResolvePhase.STATUS
//    }
//
//    if (requiredPhase == FirResolvePhase.CONTRACTS) {
//        // Workaround for recursive contracts in CLI
//        // Otherwise the assertion about presence of fir.session.phaseManager would fail
//        // See org.jetbrains.kotlin.fir.FirOldFrontendDiagnosticsTestWithStdlibGenerated.Contracts.Dsl.Errors.testRecursiveContract
//        if (fir.session.phaseManager == null) return
//    }

    val requiredPhase = FirResolvePhase.DECLARATIONS

    ensureResolved(requiredPhase, useSiteSession)
}

fun ConeKotlinType.ensureResolvedTypeDeclaration(
    useSiteSession: FirSession,
) {
    if (this !is ConeClassLikeType) return

    lookupTag.toSymbol(useSiteSession)?.ensureResolved(FirResolvePhase.DECLARATIONS, useSiteSession)
    fullyExpandedType(useSiteSession).lookupTag.toSymbol(useSiteSession)?.ensureResolved(FirResolvePhase.DECLARATIONS, useSiteSession)
}

fun FirTypeRef.ensureResolvedTypeDeclaration(
    useSiteSession: FirSession,
) {
    coneTypeSafe<ConeKotlinType>()?.ensureResolvedTypeDeclaration(useSiteSession)
}
