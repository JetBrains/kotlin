/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

class ByteArrayCharSequence(
    private val bytes: ByteArray,
    private val start: Int = 0,
    private val end: Int = bytes.size
) : CharSequence {

    override fun hashCode(): Int {
        error("Do not try computing hashCode ByteArrayCharSequence")
    }

    override fun equals(other: Any?): Boolean {
        error("Do not try comparing ByteArrayCharSequence")
    }

    override val length get() = end - start

    override fun get(index: Int): Char = bytes[index + start].toChar()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex == 0 && endIndex == length) return this
        return ByteArrayCharSequence(bytes, start + startIndex, start + endIndex)
    }

    override fun toString(): String {
        val chars = CharArray(length)

        for (i in 0 until length) {
            chars[i] = bytes[i + start].toChar()
        }

        return String(chars)
    }
}

