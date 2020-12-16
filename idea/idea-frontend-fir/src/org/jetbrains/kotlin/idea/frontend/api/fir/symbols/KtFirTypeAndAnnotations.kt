/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.firRef
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal class KtFirTypeAndAnnotations(
    containingDeclaration: FirDeclaration,
    receiverTypeRef: FirTypeRef,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtTypeAndAnnotations() {

    private val containingDeclarationRef = firRef(containingDeclaration, resolveState)
    private val receiverWeakTypeRef by weakRef(receiverTypeRef)

    override val type: KtType by containingDeclarationRef.withFirAndCache(FirResolvePhase.TYPES) {
        builder.buildKtType(receiverWeakTypeRef)
    }

    override val annotations: List<KtAnnotationCall> by containingDeclarationRef.withFirAndCache { fir ->
        receiverWeakTypeRef.annotations.map {
            KtFirAnnotationCall(fir, it, resolveState, token)
        }
    }
}