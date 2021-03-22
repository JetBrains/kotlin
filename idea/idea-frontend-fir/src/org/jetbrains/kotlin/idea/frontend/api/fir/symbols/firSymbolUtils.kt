/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolModality
import org.jetbrains.kotlin.metadata.ProtoBuf

internal inline fun <reified M : KtSymbolModality> Modality?.getSymbolModality(): M = when (this) {
    Modality.FINAL -> KtCommonSymbolModality.FINAL
    Modality.OPEN -> KtCommonSymbolModality.OPEN
    Modality.ABSTRACT -> KtCommonSymbolModality.ABSTRACT
    Modality.SEALED -> KtSymbolModality.SEALED
    null -> error("Symbol modality should not be null, looks like the fir symbol was not properly resolved")
} as? M ?: error("Sealed modality can only be applied to class")

internal inline fun <F : FirMemberDeclaration, reified M : KtSymbolModality> KtFirSymbol<F>.getModality() =
    firRef.withFir(FirResolvePhase.STATUS) { it.modality.getSymbolModality<M>() }


internal fun <F : FirMemberDeclaration> KtFirSymbol<F>.getVisibility(): Visibility =
    firRef.withFir(FirResolvePhase.STATUS) { fir -> fir.visibility }


internal fun ConeClassLikeType.expandTypeAliasIfNeeded(session: FirSession): ConeClassLikeType {
    val firTypeAlias = lookupTag.toSymbol(session) as? FirTypeAliasSymbol ?: return this
    val expandedType = firTypeAlias.fir.expandedTypeRef.coneType
    return expandedType.fullyExpandedType(session) as? ConeClassLikeType
        ?: return this
}