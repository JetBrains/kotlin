/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirAttachment
import org.jetbrains.kotlin.analysis.utils.errors.withPsiAttachment
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.withAttachmentDetailed


fun KotlinExceptionWithAttachments.withSymbolAttachment(name: String, symbol: KtSymbol): KotlinExceptionWithAttachments {
    withAttachmentDetailed(name, symbol, DebugSymbolRenderer::render)
    withPsiAttachment("${name}Psi", symbol.psi)

    if (symbol is KtFirSymbol<*>) {
        val symbolFir = symbol.firSymbol.fir
        withFirAttachment("${name}Fir", symbolFir)
    }
    return this
}