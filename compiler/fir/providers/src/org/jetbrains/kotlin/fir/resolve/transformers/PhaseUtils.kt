/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

fun ConeKotlinType?.ensureResolvedTypeDeclaration(
    useSiteSession: FirSession,
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS,
) {
    if (this !is ConeClassLikeType) return

    lookupTag.toSymbol(useSiteSession)?.lazyResolveToPhase(requiredPhase)
    fullyExpandedType(useSiteSession).lookupTag.toSymbol(useSiteSession)?.lazyResolveToPhase(requiredPhase)
}

fun FirTypeRef.ensureResolvedTypeDeclaration(
    useSiteSession: FirSession,
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS,
) {
    coneTypeSafe<ConeKotlinType>().ensureResolvedTypeDeclaration(useSiteSession, requiredPhase)
}
