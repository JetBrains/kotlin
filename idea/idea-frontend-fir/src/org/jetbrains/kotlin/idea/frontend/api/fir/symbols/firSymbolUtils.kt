/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.util.getElementTextInContext
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtDeclaration

internal fun KtFirSymbol<FirMemberDeclaration>.getModality(
    phase: FirResolvePhase = FirResolvePhase.STATUS,
    defaultModality: Modality? = null
) =
    firRef.withFir(phase) { fir ->
        fir.modality
            ?: defaultModality
            ?: fir.invalidModalityError()
    }


private fun FirDeclaration.invalidModalityError(): Nothing {
    error(
        """|Symbol modality should not be null, looks like the FIR symbol was not properly resolved
                   |
                   |${renderWithType(FirRenderer.RenderMode.WithResolvePhases)}
                   |
                   |${(psi as? KtDeclaration)?.getElementTextInContext()}""".trimMargin()
    )
}


internal fun <F : FirMemberDeclaration> KtFirSymbol<F>.getVisibility(phase: FirResolvePhase = FirResolvePhase.STATUS): Visibility =
    firRef.withFir(phase) { fir -> fir.visibility }

internal fun KtFirSymbol<FirCallableDeclaration<*>>.getCallableIdIfNonLocal(): CallableId? =
    firRef.withFir { fir -> fir.symbol.callableId.takeUnless { it.isLocal } }
