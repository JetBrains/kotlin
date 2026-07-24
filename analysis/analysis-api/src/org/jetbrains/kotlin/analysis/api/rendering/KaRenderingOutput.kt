/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * A destination for rendered output. A [KaRenderer] writes text fragments, each tagged with [KaTextAttribute]s that describe
 * its semantic role, so a output may add syntax highlighting, produce plain text, or ignore the attributes entirely.
 *
 * All methods return `this` to allow chaining.
 */
@KaExperimentalApi
public interface KaRenderingOutput {
    /** Appends [text] tagged with the given [attributes]. */
    public fun append(text: String, attributes: Set<KaTextAttribute>): KaRenderingOutput

    /** Appends [text] tagged with a single [attribute]. */
    public fun append(text: String, attribute: KaTextAttribute): KaRenderingOutput {
        return append(text, setOf(attribute))
    }

    /** Increases the indentation level applied at the start of subsequent lines. */
    public fun indent(): KaRenderingOutput

    /** Decreases the indentation level. Must be balanced with a preceding [indent]. */
    public fun unindent(): KaRenderingOutput

    /** Starts a new line at the current indentation level. */
    public fun newLine(): KaRenderingOutput

    /** Appends a single space. */
    public fun space(): KaRenderingOutput

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

/** Describes the semantic role of a fragment of rendered text, e.g. for syntax highlighting. */
@KaExperimentalApi
public sealed interface KaTextAttribute {
    /** A hard keyword or soft/modifier keyword, such as `fun`, `val`, or `suspend`. */
    @KaExperimentalApi
    public object Keyword : KaTextAttribute

    /** Punctuation, such as `:`, `,`, or `?`. */
    @KaExperimentalApi
    public object Punctuation : KaTextAttribute

    /** An opening bracket of a group, such as `(`, `<`, or `{`. */
    @KaExperimentalApi
    public object GroupStart : KaTextAttribute

    /** A closing bracket of a group, such as `)`, `>`, or `}`. */
    @KaExperimentalApi
    public object GroupEnd : KaTextAttribute

    /** An identifier, such as a declaration or type name. See [Symbol] when the referenced symbol is known. */
    @KaExperimentalApi
    public object Identifier : KaTextAttribute

    /** Whitespace that is not a line break. */
    @KaExperimentalApi
    public object Whitespace : KaTextAttribute

    /** A string or character literal, such as `"text"` or `'c'`. */
    @KaExperimentalApi
    public object StringLiteral : KaTextAttribute

    /** A numeric literal, such as `42`, `2f`, or `3u`. */
    @KaExperimentalApi
    public object NumberLiteral : KaTextAttribute

    /** An identifier that references a known [symbol]. */
    @KaExperimentalApi
    public class Symbol(public val symbol: KaSymbol) : KaTextAttribute, KaLifetimeOwner {
        override val token: KaLifetimeToken
            get() = symbol.token
    }
}
