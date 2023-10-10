/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.printer

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
public class PrettyPrinter(public val indentSize: Int = 2) : Appendable {
    @PublishedApi
    internal val builder: StringBuilder = StringBuilder()

    @PublishedApi
    internal var prefixesToPrint: PersistentList<String> = persistentListOf()

    @PublishedApi
    internal var indent: Int = 0

    override fun append(seq: CharSequence): Appendable = apply {
        if (seq.isEmpty()) return@apply
        printPrefixes()
        seq.split('\n').forEachIndexed { index, line ->
            if (index > 0) {
                builder.append('\n')
            }
            appendIndentIfNeeded()
            builder.append(line)
        }
    }

    override fun append(seq: CharSequence, start: Int, end: Int): Appendable = apply {
        append(seq.subSequence(start, end))
    }

    override fun append(c: Char): Appendable = apply {
        printPrefixes()
        if (c != '\n') {
            appendIndentIfNeeded()
        }
        builder.append(c)
    }

    private fun printPrefixes() {
        if (prefixesToPrint.isNotEmpty()) {
            appendIndentIfNeeded()
            prefixesToPrint.forEach { builder.append(it) }
            prefixesToPrint = persistentListOf()
        }
    }

    public inline fun withIndent(block: PrettyPrinter.() -> Unit) {
        indent += 1
        block(this)
        indent -= 1
    }

    public inline fun withIndents(indentCount: Int, block: PrettyPrinter.() -> Unit) {
        require(indentCount >= 0) { "Number of indents should be non-negative" }
        indent += indentCount
        block(this)
        indent -= indentCount
    }

    public inline fun withIndentInBraces(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "{", after = "}", block)
    }

    public inline fun withIndentInSquareBrackets(block: PrettyPrinter.() -> Unit) {
        withIndentWrapped(before = "[", after = "]", block)
    }

    public inline fun withIndentWrapped(before: String, after: String, block: PrettyPrinter.() -> Unit) {
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
        if (builder.lastOrNull() != char) {
            append(char)
        }
    }

    private fun appendIndentIfNeeded() {
        if (builder.isEmpty() || builder[builder.lastIndex] == '\n') {
            builder.append(" ".repeat(indentSize * indent))
        }
    }

    override fun toString(): String {
        return builder.toString()
    }

    public inline fun checkIfPrinted(render: () -> Unit): Boolean {
        contract { callsInPlace(render, InvocationKind.EXACTLY_ONCE) }
        val initialSize = builder.length
        render()
        return initialSize != builder.length
    }

    public inline operator fun invoke(print: PrettyPrinter.() -> Unit) {
        this.print()
    }

    public inline fun String.separated(p1: () -> Unit, p2: () -> Unit) {
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

    public inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2) }, p3)
    }

    public inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p4, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3) }, p4)
    }

    public inline fun String.separated(p1: () -> Unit, p2: () -> Unit, p3: () -> Unit, p4: () -> Unit, p5: () -> Unit) {
        contract {
            callsInPlace(p1, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p2, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p3, InvocationKind.EXACTLY_ONCE)
            callsInPlace(p5, InvocationKind.EXACTLY_ONCE)
        }
        separated({ separated(p1, p2, p3, p4) }, p5)
    }

    public inline fun withPrefix(prefix: String, print: () -> Unit) {
        contract {
            callsInPlace(print, InvocationKind.EXACTLY_ONCE)
        }
        val currentPrefixes = prefixesToPrint
        prefixesToPrint = prefixesToPrint.add(prefix)
        try {
            print()
        } finally {
            if (prefixesToPrint.isNotEmpty()) {
                prefixesToPrint = currentPrefixes
            }
        }
    }

    public inline fun withSuffix(suffix: String, p1: () -> Unit) {
        checkIfPrinted { p1() }.ifTrue { append(suffix) }
    }
}

public inline fun prettyPrint(body: PrettyPrinter.() -> Unit): String =
    PrettyPrinter().apply(body).toString()

@OptIn(ExperimentalContracts::class)
public inline fun prettyPrintWithSettingsFrom(other: PrettyPrinter, body: PrettyPrinter.() -> Unit): String {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return PrettyPrinter(other.indentSize).apply(body).toString()
}
