/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * A destination for rendered output. A [KaRenderer] writes text fragments, each tagged with [KaTextAttribute]s that describe
 * its semantic role, so a output may add syntax highlighting, produce plain text, or ignore the attributes entirely.
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
    public fun indent(): KaRenderingOutput

    /** Decreases the indentation level. Must be balanced with a preceding [indent]. */
    public fun unindent(): KaRenderingOutput

    /** Starts a new line at the current indentation level. */
    public fun newLine(): KaRenderingOutput

    /**
     * Called when rendering of [piece] begins. Everything appended until the balancing [leave] call is the output of that piece; the
     * calls nest when a piece renders other pieces.
     */
    public fun enter(piece: KaPiece<*>) {}

    /** Called when rendering of [piece] ends. Balances the corresponding [enter] call, also when rendering fails with an exception. */
    public fun leave(piece: KaPiece<*>) {}

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
            return KaStringRenderingOutput(indentationUnit = "    ")
        }

        /**
         * A simple [KaRenderingOutput] that accumulates rendered text into a plain [String], ignoring all [KaTextAttribute]s.
         * Uses the [indentationUnit] for member indentation.
         *
         * The rendered text is available via [toString].
         */
        public fun plainString(indentationUnit: String): KaRenderingOutput {
            return KaStringRenderingOutput(indentationUnit)
        }
    }
}

/** Appends [text] tagged with the given [attributes]. */
@KaExperimentalApi
public fun KaRenderingOutput.append(text: String, vararg attributes: KaTextAttribute): KaRenderingOutput {
    return append(text, attributes.toSet())
}

/**
 * A convenience property allowing to get the [KaRenderingOutput] inside the rendering block without `contextOf<KaRenderingOutput>()`.
 */
@KaExperimentalApi
context(output: KaRenderingOutput)
public val output: KaRenderingOutput
    get() = output

/** Describes the semantic role of a fragment of rendered text, e.g. for syntax highlighting. */
@KaSpi
@KaExperimentalApi
public interface KaTextAttribute : KaLifetimeOwner {
    /** A hard keyword or soft/modifier keyword, such as `fun`, `val`, or `suspend`. */
    @KaExperimentalApi
    public object Keyword : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** Punctuation, such as `:`, `,`, or `?`. */
    @KaExperimentalApi
    public object Punctuation : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** An identifier, such as a declaration or type name. See [Symbol] when the referenced symbol is known. */
    @KaExperimentalApi
    public object Identifier : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** Whitespace that is not a line break. */
    @KaExperimentalApi
    public object Whitespace : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** A string or character literal, such as `"text"` or `'c'`. */
    @KaExperimentalApi
    public object StringLiteral : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** A numeric literal, such as `42`, `2f`, or `3u`. */
    @KaExperimentalApi
    public object NumberLiteral : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** A comment, such as `/* = kotlin.String */`. */
    @KaExperimentalApi
    public object Comment : KaTextAttribute {
        override val token: KaLifetimeToken get() = KaTextAttributeLifetimeToken
    }

    /** An identifier that references a known [symbol]. Only attached when [KaRenderingOption.LinkSymbols] is enabled. */
    @KaExperimentalApi
    public class Symbol(public val symbol: KaSymbol) : KaTextAttribute {
        override val token: KaLifetimeToken
            get() = symbol.token

        override fun equals(other: Any?): Boolean = (this === other) || (other is Symbol && symbol == other.symbol)
        override fun hashCode(): Int = symbol.hashCode()
        override fun toString(): String = "KaTextAttribute.Symbol(symbol=$symbol)"
    }
}

@OptIn(KaPlatformInterface::class)
private object KaTextAttributeLifetimeToken : KaLifetimeToken() {
    override fun isValid() = true
    override fun isAccessible() = true

    override fun getInvalidationReason() = error("Getting invalidation reason for a valid token")
    override fun getInaccessibilityReason() = error("Getting inaccessibility reason for a valid token")
}
