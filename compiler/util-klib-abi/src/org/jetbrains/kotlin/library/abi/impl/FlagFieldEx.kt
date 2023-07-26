/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.metadata.deserialization.Flags.FlagField

/** TODO: Think how to contribute it to [FlagField]. */
internal abstract class FlagFieldEx<T>(val offset: Int, val bitWidth: Int) {
    init {
        require(offset >= 0) { "Invalid offset: $offset" }
        require(bitWidth > 0) { "Invalid bit width: $bitWidth" }

        val overflow = offset + bitWidth - Int.SIZE_BITS
        require(overflow <= 0) { "Not enough bit space for storage. Offset=$offset, width=$bitWidth, overflow=$overflow." }
    }

    abstract fun get(flags: Int): T
    abstract fun toFlags(value: T): Int

    private val bitMask = (1 shl bitWidth) - 1

    protected fun readSignificantBitsFromFlags(flags: Int): Int = (flags ushr offset) and bitMask

    protected fun storeSignificantBitsAsFlags(value: Int): Int {
        require((value ushr bitWidth) == 0) { "Not enough space to store $value" }
        return value shl offset
    }

    class IntFlagFieldEx(offset: Int, bitWidth: Int) : FlagFieldEx<Int>(offset, bitWidth) {
        override fun get(flags: Int): Int = readSignificantBitsFromFlags(flags)
        override fun toFlags(value: Int): Int = storeSignificantBitsAsFlags(value)
    }

    class EnumFlagFieldEx<E : Enum<E>>(offset: Int, private val entries: Array<E>) : FlagFieldEx<E>(offset, computeBitWidth(entries)) {
        override fun get(flags: Int): E = entries[readSignificantBitsFromFlags(flags)]
        override fun toFlags(value: E): Int = storeSignificantBitsAsFlags(value.ordinal)

        companion object {
            private fun <E : Enum<E>> computeBitWidth(entries: Array<E>): Int {
                require(entries.isNotEmpty()) { "No enum entries" }
                return maxOf(1, Int.SIZE_BITS - entries.lastIndex.countLeadingZeroBits())
            }
        }
    }

    companion object {
        fun intFirst(bitWidth: Int): FlagFieldEx<Int> = IntFlagFieldEx(0, bitWidth)
        fun intAfter(previous: FlagField<*>, bitWidth: Int): FlagFieldEx<Int> = IntFlagFieldEx(previous.nextOffset, bitWidth)
        fun intAfter(previous: FlagFieldEx<*>, bitWidth: Int): FlagFieldEx<Int> = IntFlagFieldEx(previous.nextOffset, bitWidth)

        inline fun <reified E : Enum<E>> first(): FlagFieldEx<E> = EnumFlagFieldEx(0, enumValues())
        inline fun <reified E : Enum<E>> after(previous: FlagField<*>): FlagFieldEx<E> = EnumFlagFieldEx(previous.nextOffset, enumValues())
        inline fun <reified E : Enum<E>> after(previous: FlagFieldEx<*>): FlagFieldEx<E> = EnumFlagFieldEx(previous.nextOffset, enumValues())

        private val FlagField<*>.nextOffset: Int get() = offset + bitWidth
        private val FlagFieldEx<*>.nextOffset: Int get() = offset + bitWidth
    }
}
