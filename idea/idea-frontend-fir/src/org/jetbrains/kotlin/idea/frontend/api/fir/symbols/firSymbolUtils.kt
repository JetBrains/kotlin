/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolVisibility

internal inline fun <reified M : KtSymbolModality> Modality?.getSymbolModality(): M = when (this) {
    Modality.FINAL -> KtCommonSymbolModality.FINAL
    Modality.OPEN -> KtCommonSymbolModality.OPEN
    Modality.ABSTRACT -> KtCommonSymbolModality.ABSTRACT
    Modality.SEALED -> KtSymbolModality.SEALED
    null -> error("Symbol modality should not be null, looks like the fir symbol was not properly resolved")
} as? M ?: error("Sealed modality can only be applied to class")

internal inline fun <F : FirMemberDeclaration, reified M : KtSymbolModality> KtFirSymbol<F>.getModality() =
    firRef.withFir(FirResolvePhase.STATUS) { it.modality.getSymbolModality<M>() }


internal fun Visibility?.getSymbolVisibility(): KtSymbolVisibility = when (this) {
    Visibilities.Public -> KtSymbolVisibility.PUBLIC
    Visibilities.Protected -> KtSymbolVisibility.PROTECTED
    Visibilities.Private -> KtSymbolVisibility.PRIVATE
    Visibilities.Internal -> KtSymbolVisibility.INTERNAL
    Visibilities.Local -> KtSymbolVisibility.LOCAL
    Visibilities.Unknown -> KtSymbolVisibility.UNKNOWN
    JavaVisibilities.PackageVisibility -> KtSymbolVisibility.UNKNOWN //TODO: Add Java visibilities
    null -> error("Symbol visibility should not be null, looks like the fir symbol was not properly resolved")
    else -> throw NotImplementedError("Unknown visibility $name")
}

internal fun <F : FirMemberDeclaration> KtFirSymbol<F>.getVisibility(): KtSymbolVisibility =
    firRef.withFir(FirResolvePhase.STATUS) { it.visibility.getSymbolVisibility() }