/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.components.KaFileAnnotationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirFileLevelAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fir.declarations.utils.klibFileAnnotations

internal class KaFirFileAnnotationProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaFileAnnotationProvider {
    override val KaDeclarationSymbol.fileAnnotations: KaAnnotationList
        get() = withValidityAssertion {
            if (!isTopLevel) {
                return@withValidityAssertion KaBaseEmptyAnnotationList(analysisSession.firSymbolBuilder.token)
            }

            firSymbol.fir.getContainingFile()?.let {
                return@withValidityAssertion KaFirAnnotationListForDeclaration.create(it.symbol, analysisSession.firSymbolBuilder)
            }

            KaFirFileLevelAnnotationList.create(firSymbol.klibFileAnnotations, analysisSession.firSymbolBuilder)
        }
}
