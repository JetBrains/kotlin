/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

fun FirBasedSymbol<*>.ensureResolvedForCalls(
    useSiteSession: FirSession,
) {
    if (fir.resolvePhase >= FirResolvePhase.DECLARATIONS) return

//    val requiredPhase = when (fir) {
//        is FirFunction, is FirProperty -> FirResolvePhase.CONTRACTS
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
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS,
) {
    if (this !is ConeClassLikeType) return

    lookupTag.toSymbol(useSiteSession)?.ensureResolved(requiredPhase, useSiteSession)
    fullyExpandedType(useSiteSession).lookupTag.toSymbol(useSiteSession)?.ensureResolved(requiredPhase, useSiteSession)
}

fun FirTypeRef.ensureResolvedTypeDeclaration(
    useSiteSession: FirSession,
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS,
) {
    coneTypeSafe<ConeKotlinType>()?.ensureResolvedTypeDeclaration(useSiteSession, requiredPhase)
}
