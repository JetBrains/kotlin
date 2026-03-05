/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.internal.utils

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** DSL marker for [IndentedTextBuilder] scope, preventing implicit receiver leaking into nested lambdas. */
@DslMarker
@Target(AnnotationTarget.CLASS)
public annotation class IndentingPrinterDsl

/**
 * An indentation-aware text builder with a DSL interface.
 *
 * Unlike [org.jetbrains.kotlin.utils.Printer] and [org.jetbrains.kotlin.utils.SmartPrinter],
 * this class correctly handles indentation in all cases, including multi-line values and conditional prefixes/suffixes.
 *
 * Use [buildIndentedText] as a convenient entry point.
 *
 * @param indentation the string used for a single indentation level (default: four spaces).
 * @param lineSeparator the line separator appended by [appendLine] (default: `"\n"`).
 */
@IndentingPrinterDsl
@OptIn(ExperimentalContracts::class)
public class IndentedTextBuilder(
    private val indentation: String = FOUR_SPACES,
    private val lineSeparator: String = "\n",
) {
    private val result: StringBuilder = StringBuilder()
    private var indentationLevel: Int = 0
    private var pendingPrefixes: MutableList<String> = mutableListOf()

    /**
     * Appends [value] to the output, applying the current indentation at the start of each non-empty line.
     *
     * Multi-line values are split on newlines and each line is indented independently.
     * Empty strings are ignored.
     *
     * Returns `this` for chaining.
     */
    public fun append(value: Any?): IndentedTextBuilder {
        val text = value.toString()
        if (text.isEmpty()) {
            return this
        }

        flushPrefixes()

        for ((index, line) in text.lines().withIndex()) {
            if (index > 0) {
                result.append(lineSeparator)
            }

            if (line.isNotEmpty()) {
                appendIndentationIfNeeded()
                result.append(line)
            }
        }

        return this
    }

    /**
     * Appends [value] followed by [lineSeparator].
     * Equivalent to `append(value).append(lineSeparator)`.
     */
    public fun appendLine(value: Any? = ""): IndentedTextBuilder {
        append(value)
        append(lineSeparator)
        return this
    }

    private fun flushPrefixes() {
        if (pendingPrefixes.isNotEmpty()) {
            appendIndentationIfNeeded()
            pendingPrefixes.forEach(result::append)
            pendingPrefixes.clear()
        }
    }

    /**
     * Increases the indentation level by one for the duration of [block], then restores it.
     */
    public fun withIndent(block: IndentedTextBuilder.() -> Unit) {
        indentationLevel += 1
        block(this)
        indentationLevel -= 1
    }

    /**
     * Renders [collection] with each item separated by [separator], wrapped in [prefix] and [postfix].
     * If [skipIfEmpty] is `true` and the collection is empty, nothing is appended.
     * Each item is rendered by [renderItem] (defaults to [append]).
     */
    public fun <T> appendCollection(
        collection: Collection<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        skipIfEmpty: Boolean = false,
        renderItem: IndentedTextBuilder.(T) -> Unit = { append(it) }
    ) {
        if (skipIfEmpty && collection.isEmpty()) {
            return
        }

        append(prefix)
        val iterator = collection.iterator()
        while (iterator.hasNext()) {
            renderItem(iterator.next())
            if (iterator.hasNext()) {
                append(separator)
            }
        }
        append(postfix)
    }

    /**
     * Renders each block in [blocks], inserting [separator] between consecutive blocks that produce output.
     * Blocks that produce no output are skipped without emitting a separator.
     */
    public fun appendBlocks(separator: String, vararg blocks: IndentedTextBuilder.() -> Unit) {
        if (blocks.isEmpty()) {
            return
        }

        var needsSeparator = false
        for (item in blocks) {
            val newNeedsSeparator = hasPrinted {
                if (needsSeparator) {
                    withPrefix(separator, item)
                } else {
                    item()
                }
            }
            needsSeparator = needsSeparator || newNeedsSeparator
        }
    }

    /**
     * Runs [block] and returns `true` if it produced any output.
     */
    public fun hasPrinted(block: IndentedTextBuilder.() -> Unit): Boolean {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val initialSize = result.length
        block()
        return initialSize != result.length
    }

    /**
     * Prepends [prefix] to the first text appended inside [block].
     * If [block] produces no output, [prefix] is discarded.
     */
    public fun withPrefix(prefix: String, block: IndentedTextBuilder.() -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        if (prefix.isEmpty()) {
            block()
            return
        }

        val prefixCountBefore = pendingPrefixes.size
        pendingPrefixes.add(prefix)
        block()

        if (pendingPrefixes.isNotEmpty()) {
            pendingPrefixes.subList(prefixCountBefore, pendingPrefixes.size).clear()
        }
    }

    /**
     * Appends [suffix] after [block], but only if [block] produced any output.
     */
    public fun withSuffix(suffix: String, block: IndentedTextBuilder.() -> Unit) {
        if (hasPrinted { block() }) {
            append(suffix)
        }
    }

    private fun appendIndentationIfNeeded() {
        val needsIndentation = result.isEmpty()
                || result.last().let { it == '\n' || it == '\r' }

        if (needsIndentation) {
            result.append(indentation.repeat(indentationLevel))
        }
    }

    override fun toString(): String {
        return result.toString()
    }

    public companion object {
        public const val TWO_SPACES: String = "  "
        public const val FOUR_SPACES: String = "    "
    }
}

/**
 * Creates a [IndentedTextBuilder], runs [body] on it, and returns the resulting string.
 */
public inline fun buildIndentedText(
    indentation: String = IndentedTextBuilder.FOUR_SPACES,
    lineSeparator: String = "\n",
    body: IndentedTextBuilder.() -> Unit
): String =
    IndentedTextBuilder(indentation, lineSeparator).apply(body).toString()
