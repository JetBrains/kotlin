/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry


fun ExceptionAttachmentBuilder.withSymbolAttachment(name: String, symbol: KtSymbol, analysisSession: KtAnalysisSession) {
    with(analysisSession) {
        withEntry(name, symbol) { DebugSymbolRenderer(renderExtra = true).render(it) }
    }

    val psi = symbol.psi
    val psiModule = psi?.let(analysisSession::getModule)
    withPsiEntry("${name}Psi", psi, psiModule)

    if (symbol is KtFirSymbol<*>) {
        val symbolFir = symbol.firSymbol.fir
        withFirEntry("${name}Fir", symbolFir)
    }
}