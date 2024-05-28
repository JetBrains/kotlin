/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSamResolver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.getClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.FirSamResolver

internal class KaFirSamResolver(override val analysisSession: KaFirSession) : KaSamResolver(), KaFirSessionComponent {
    override fun getSamConstructor(symbol: KaClassLikeSymbol): KaSamConstructorSymbol? {
        val classId = symbol.classId ?: return null
        val owner = analysisSession.getClassLikeSymbol(classId) ?: return null
        val firSession = analysisSession.useSiteSession
        val resolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))
        return resolver.getSamConstructor(owner)?.let {
            analysisSession.firSymbolBuilder.functionLikeBuilder.buildSamConstructorSymbol(it.symbol)
        }
    }
}
