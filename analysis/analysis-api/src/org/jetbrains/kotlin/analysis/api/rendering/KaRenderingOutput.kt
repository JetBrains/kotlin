/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsRendererProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.render
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A destination for rendered output. A [KaRenderer] writes text fragments, each tagged with [KaTextAttribute]s that describe
 * its semantic role, so an output may add syntax highlighting, produce plain text, or ignore the attributes entirely.
 *
 * All methods return `this` to allow chaining.
 */
@KaSpi
@KaExperimentalApi
public interface KaRenderingOutput {
    /** Appends [text] tagged with the given [attributes]. */
    public fun append(text: String, attributes: Set<KaTextAttribute>): KaRenderingOutput

    /** Append a single space unless the last rendered character was a whitespace. */
    public fun space(): KaRenderingOutput

    /** Increases the indentation level applied at the start of subsequent lines. */
    public fun pushIndent(): KaRenderingOutput

    /** Decreases the indentation level. Must be balanced with a preceding [pushIndent]. */
    public fun popIndent(): KaRenderingOutput

    /** Starts a new line at the current indentation level. */
    public fun newLine(): KaRenderingOutput

    /**
     * Called when rendering of [piece] begins. Everything appended until the balancing [leave] call is the output of that piece; the
     * calls nest when a piece renders other pieces.
     */
    @KaSpiExtensionPoint
    public fun enter(piece: KaPiece<*>) {
    }

    /** Called when rendering of [piece] ends. Balances the corresponding [enter] call, also when rendering fails with an exception. */
    @KaSpiExtensionPoint
    public fun leave(piece: KaPiece<*>) {
    }

    /**
     * Renders a region which groups a sequence of child pieces of the [children] kind, such as a value parameter list grouping its
     * [KaPiece.ValueParameter]s.
     *
     * [block] renders the entire content of the group, including the enclosing brackets and the separators between the children.
     * The hook lets an output lay the group out as a whole, e.g. to fold it.
     *
     * An output which needs no special treatment of groups invokes [block] as it is.
     */
    public fun group(children: KaPiece<*>, block: () -> Unit) {
        block()
    }

    @KaExperimentalApi
    public companion object {
        /**
         * A simple [KaRenderingOutput] that accumulates rendered text into a plain [String], ignoring all [KaTextAttribute]s.
         * Uses four spaces for member indentation.
         *
         * The rendered text is available via [toString].
         */
        public fun plainString(): KaRenderingOutput {
            return plainString(indentationUnit = "    ")
        }

        /**
         * A simple [KaRenderingOutput] that accumulates rendered text into a plain [String], ignoring all [KaTextAttribute]s.
         * Uses the [indentationUnit] for member indentation.
         *
         * The rendered text is available via [toString].
         */
        public fun plainString(indentationUnit: String): KaRenderingOutput {
            @OptIn(KaImplementationDetail::class)
            return service<KaInternalsRendererProvider>().createStringRenderingOutput(indentationUnit)
        }
    }
}

/** Appends [text] tagged with the given [attributes]. */
@KaExperimentalApi
public fun KaRenderingOutput.append(text: String, vararg attributes: KaTextAttribute): KaRenderingOutput {
    return append(text, attributes.toSet())
}

/**
 * Runs [block] with the indentation level increased, balancing [pushIndent] with [popIndent].
 */
@KaExperimentalApi
public inline fun KaRenderingOutput.withIndent(block: () -> Unit) {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    pushIndent()
    try {
        block()
    } finally {
        popIndent()
    }
}

/**
 * A convenience property allowing to get the [KaRenderingOutput] inside the rendering block without `contextOf<KaRenderingOutput>()`.
 */
@KaExperimentalApi
context(output: KaRenderingOutput)
public val output: KaRenderingOutput
    get() = output

/** Appends [text] as a [KaTextAttribute.Punctuation] fragment, such as `(` or `.`. */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun KaRenderingOutput.punctuation(text: String): KaRenderingOutput =
    append(text, KaTextAttribute.Punctuation)

/**
 * Renders [token] unless it is filtered out by [KaRenderingOption.AllowedKeywords].
 *
 * @param trailingSpace whether a space is appended after the keyword. Pass `false` when the keyword is directly followed by punctuation,
 * as in `constructor(`.
 */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun KaRenderingOutput.keyword(token: KtKeywordToken, trailingSpace: Boolean = true): KaRenderingOutput {
    if (!context.valueFor(KaRenderingOption.AllowedKeywords)(token)) return this

    append(token.value, KaTextAttribute.Keyword)
    if (trailingSpace) space()
    return this
}

/** Renders [name] as an identifier, linked to [symbol] when [KaRenderingOption.LinkSymbols] is enabled. */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun KaRenderingOutput.identifier(name: String, symbol: KaSymbol): KaRenderingOutput {
    return if (context.valueFor(KaRenderingOption.LinkSymbols)) {
        append(name, setOf(KaTextAttribute.Identifier, KaTextAttribute.Symbol(symbol)))
    } else {
        append(name, KaTextAttribute.Identifier)
    }
}

/** Renders [name] as an identifier, linked to [symbol] when [KaRenderingOption.LinkSymbols] is enabled. */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun KaRenderingOutput.identifier(name: Name, symbol: KaSymbol): KaRenderingOutput {
    if (name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
        return identifier("_", symbol)
    }

    // `Name.render()` wraps keywords and names with special characters (e.g. `<set-?>`) in backticks.
    return identifier(name.render(), symbol)
}
