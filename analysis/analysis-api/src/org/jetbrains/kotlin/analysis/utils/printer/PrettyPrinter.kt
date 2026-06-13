/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.printer

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public class PrettyPrinter(public val indentSize: Int = 2) : Appendable {
    private val result: StringBuilder = StringBuilder()
    private var prefixes: PersistentList<String> = persistentListOf()
    private var indentationLevel: Int = 0

    @Deprecated("Do not use. Left for binary compatibility", level = DeprecationLevel.HIDDEN)
    public val builder: StringBuilder
        get() = result

    @Deprecated("Do not use. Left for binary compatibility", level = DeprecationLevel.HIDDEN)
    public var prefixesToPrint: PersistentList<String>
        get() = prefixes
        set(value) {
            prefixes = value
        }

    @Deprecated("Do not use. Left for binary compatibility", level = DeprecationLevel.HIDDEN)
    public var indent: Int
        get() = indentationLevel
        set(value) {
            indentationLevel = value
        }

    override fun append(nullableSeq: CharSequence?): Appendable = apply {
        val seq = nullableSeq ?: "null"
        if (seq.isEmpty()) return@apply
        printPrefixes()
        seq.split('\n').forEachIndexed { index, line ->
            if (index > 0) {
                result.append('\n')
            }

            // Skip indents if the line is empty.
            if (line.isNotEmpty()) {
                appendIndentIfNeeded()
                result.append(line)
            }
        }
    }

    override fun append(nullableSeq: CharSequence?, start: Int, end: Int): Appendable = apply {
        append((nullableSeq ?: "null").subSequence(start, end))
    }

    override fun append(c: Char): Appendable = apply {
        printPrefixes()
        if (c != '\n') {
            appendIndentIfNeeded()
        }
        result.append(c)
    }

    private fun printPrefixes() {
        if (prefixes.isNotEmpty()) {
            appendIndentIfNeeded()
            prefixes.forEach { result.append(it) }
            prefixes = persistentListOf()
        }
    }

    public fun withIndent(block: PrettyPrinter.() -> Unit) {
        indentationLevel += 1
        block(this)
        indentationLevel -= 1
    }

    public fun withIndents(indentCount: Int, block: PrettyPrinter.() -> Unit) {
        require(indentCount >= 0) { "Number of indents should be non-negative" }
        indentationLevel += indentCount
        block(this)
        indentationLevel -= indentCount
    }

    public fun withIndentInBraces(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "{", after = "}", block)
    }

    public fun withIndentInSquareBrackets(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "[", after = "]", block)
    }

    public fun withIndentWrapped(before: String, after: String, block: PrettyPrinter.() -> Unit) {
        append(before)
        appendLine()
        withIndent(block)
        appendLine()
        append(after)
    }

    public inline fun <T> printCollection(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
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


    public inline fun <T> printCollectionIfNotEmpty(
        collection: Iterable<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        renderItem: PrettyPrinter.(T) -> Unit
    ) {
        if (!collection.iterator().hasNext()) return
        printCollection(collection, separator, prefix, postfix, renderItem)
    }

    public fun printCharIfNotThere(char: Char) {
        if (result.lastOrNull() != char) {
            append(char)
        }
    }

    private fun appendIndentIfNeeded() {
        if (result.isEmpty() || result[result.lastIndex] == '\n') {
            result.append(" ".repeat(indentSize * indentationLevel))
        }
    }

    override fun toString(): String {
        return result.toString()
    }

    public fun checkIfPrinted(render: () -> Unit): Boolean {
        contract { callsInPlace(render, InvocationKind.EXACTLY_ONCE) }
        val initialSize = result.length
        render()
        return initialSize != result.length
    }

    public inline operator fun invoke(print: PrettyPrinter.() -> Unit) {
        this.print()
    }

    public fun String.separated(p1: () -> Unit, p2: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
        }
        val firstRendered = checkIfPrinted { p1() }
        if (firstRendered) {
            withPrefix(this, p2)
        } else {
            p2()
        }
    }

    public fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2) }, p3)
    }

    public fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p4, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3) }, p4)
    }

    public fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit, p5: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p5, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3, p4) }, p5)
    }

    public fun withPrefix(prefix: String, print: () -> Unit) {
        contract {
            callsInPlace(print, InvocationKind.EXACTLY_ONCE)
        }
        val currentPrefixes = prefixes
        prefixes = prefixes.add(prefix)
        try {
            print()
        } finally {
            if (prefixes.isNotEmpty()) {
                prefixes = currentPrefixes
            }
        }
    }

    public fun withSuffix(suffix: String, p1: () -> Unit) {
        checkIfPrinted { p1() }.ifTrue { append(suffix) }
    }
}

@KaExperimentalApi
public inline fun prettyPrint(body: PrettyPrinter.() -> Unit): String =
    PrettyPrinter().apply(body).toString()

@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun prettyPrintWithSettingsFrom(other: PrettyPrinter, body: PrettyPrinter.() -> Unit): String {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return PrettyPrinter(other.indentSize).apply(body).toString()
}
