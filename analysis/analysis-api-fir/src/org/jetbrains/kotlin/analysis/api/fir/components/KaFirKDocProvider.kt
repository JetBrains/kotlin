/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseKDocProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.compiledStub
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

internal class KaFirKDocProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseKDocProvider<KaFirSession>() {
    @OptIn(KtNonPublicApi::class)
    override fun findDeserializedKdocText(symbol: KaDeclarationSymbol): String? {
        if (symbol.origin != KaSymbolOrigin.LIBRARY) {
            return null
        }

        return when (val psi = symbol.psi) {
            is KtClass -> psi.compiledStub.kdocText
            is KtObjectDeclaration -> psi.compiledStub.kdocText
            is KtNamedFunction -> psi.compiledStub.kdocText
            is KtProperty -> psi.compiledStub.kdocText
            is KtPrimaryConstructor -> psi.compiledStub.kdocText
            is KtSecondaryConstructor -> psi.compiledStub.kdocText
            else -> null
        }
    }
}
