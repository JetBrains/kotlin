/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.render

/** The default renderer, with the built-in piece renderer stacks assembled from the neighboring group files. */
internal val DEFAULT_RENDERER: KaRenderer = buildRenderer(null) {
    pushSymbolRenderers()
    pushAnnotationRenderers()
    pushNameRenderers()
    pushCallableRenderers()
    pushParameterRenderers()
    pushClassifierRenderers()
    pushTypeRenderers()
}

/**
 * Renders [token] unless it is filtered out by [KaRenderingOption.AllowedKeywords].
 *
 * @param trailingSpace whether a space is appended after the keyword. Pass `false` when the keyword is directly followed by punctuation,
 * as in `constructor(`.
 */
context(context: KaRenderingContext, output: KaRenderingOutput)
internal fun keyword(token: KtKeywordToken, trailingSpace: Boolean = true) {
    if (!context.valueFor(KaRenderingOption.AllowedKeywords)(token)) return

    output.append(token.value, KaTextAttribute.Keyword)
    if (trailingSpace) output.space()
}

/** Renders [name] as an identifier, linked to [symbol] when [KaRenderingOption.LinkSymbols] is enabled. */
context(context: KaRenderingContext, output: KaRenderingOutput)
internal fun identifier(name: String, symbol: KaSymbol) {
    if (context.valueFor(KaRenderingOption.LinkSymbols)) {
        output.append(name, setOf(KaTextAttribute.Identifier, KaTextAttribute.Symbol(symbol)))
    } else {
        output.append(name, KaTextAttribute.Identifier)
    }
}

context(context: KaRenderingContext, output: KaRenderingOutput)
internal fun identifier(name: Name, symbol: KaSymbol) {
    if (name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
        identifier("_", symbol)
        return
    }

    // `Name.render()` wraps keywords and names with special characters (e.g. `<set-?>`) in backticks.
    identifier(name.render(), symbol)
}
