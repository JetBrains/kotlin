/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * A printer that is suitable for printing text with indentation.
 */
interface IndentingPrinter {

    /**
     * The current indentation level. Basically, this is the number of [pushIndent] calls that don't yet have a matching [popIndent] call.
     */
    val currentIndentLengthInUnits: Int

    /**
     * The number of characters in a single indent unit.
     */
    val indentUnitLength: Int

    /**
     * Prints [objects] by concatenating the results of their [Any.toString] calls, also appending a line break at the end.
     *
     * @return `this`
     */
    fun println(vararg objects: Any?): IndentingPrinter

    /**
     * Prints [objects] by concatenating the results of their [Any.toString] calls.
     *
     * @return `this`
     */
    fun print(vararg objects: Any?): IndentingPrinter

    /**
     * Increases the indentation level by one.
     *
     * @return `this`
     */
    fun pushIndent(): IndentingPrinter

    /**
     * Decreases the indentation level by one.
     *
     * @return `this`
     */
    fun popIndent(): IndentingPrinter

    /**
     * Returns the printed text.
     */
    override fun toString(): String
}

/**
 * The text printed within [block] will be indented.
 */
inline fun IndentingPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
