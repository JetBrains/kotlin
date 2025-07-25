/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.wasm.ir.WasmSymbol

// Representation of constant data in Wasm memory

internal const val CHAR_SIZE_BYTES = 2
internal const val BYTE_SIZE_BYTES = 1
internal const val SHORT_SIZE_BYTES = 2
internal const val INT_SIZE_BYTES = 4
internal const val LONG_SIZE_BYTES = 8

sealed class ConstantDataElement {
    abstract val sizeInBytes: Int
    abstract fun dump(indent: String = "", startAddress: Int = 0): String
    abstract fun toBytes(): ByteArray
}

private fun addressToString(address: Int): String =
    address.toString().padEnd(6, ' ')

class ConstantDataCharField(val value: WasmSymbol<Char>) : ConstantDataElement() {
    constructor(value: Char) : this(WasmSymbol(value))

    override fun toBytes(): ByteArray = ByteArray(2).apply { value.owner.toLittleEndianBytes(this, 0, false) }

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : ${value.owner}    ;;\n"
    }

    override val sizeInBytes: Int = CHAR_SIZE_BYTES
}

class ConstantDataIntField(val value: WasmSymbol<Int>) : ConstantDataElement() {
    constructor(value: Int) : this(WasmSymbol(value))

    override fun toBytes(): ByteArray = ByteArray(4).apply { value.owner.toLittleEndianBytes(this, 0) }

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : ${value.owner}    ;;\n"
    }

    override val sizeInBytes: Int = INT_SIZE_BYTES
}

class ConstantDataIntegerArray(val value: List<Long>, val integerSize: Int) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        val array = ByteArray(value.size * integerSize)
        repeat(value.size) { i ->
            value[i].toLittleEndianBytesTo(array, i * integerSize, integerSize)
        }
        return array
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i${integerSize * 8}[] : ${toBytes().contentToString()}   ;;\n"
    }

    override val sizeInBytes: Int = value.size * integerSize
}

class ConstantDataIntArray(val value: List<WasmSymbol<Int>>) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        return ByteArray(value.size * 4).apply {
            for (index in value.indices) {
                value[index].owner.toLittleEndianBytes(this, index * 4)
            }
        }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i32[] : ${value.map { it.owner }.toIntArray().contentToString()}   ;;\n"
    }

    override val sizeInBytes: Int = value.size * INT_SIZE_BYTES
}

class ConstantDataCharArray(val value: List<WasmSymbol<Char>>, val fitsLatin1: Boolean) : ConstantDataElement() {
    constructor(value: CharArray, fitsLatin1: Boolean) : this(value.map { WasmSymbol(it) }, fitsLatin1)

    override fun toBytes(): ByteArray {
        return ByteArray(value.size * bytesPerChar).apply {
            value.forEachIndexed { index, symbol -> symbol.owner.toLittleEndianBytes(this, index * bytesPerChar, fitsLatin1) }
        }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i16[] : ${value.map { it.owner }.toCharArray().contentToString()}   ;;\n"
    }

    private val bytesPerChar = if (fitsLatin1) BYTE_SIZE_BYTES else CHAR_SIZE_BYTES

    override val sizeInBytes: Int = value.size * bytesPerChar

}

class ConstantDataStruct(val elements: List<ConstantDataElement>) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        return buildList {
            elements.forEach {
                for (byte in it.toBytes()) {
                    add(byte)
                }
            }
        }.toByteArray()
    }

    override fun dump(indent: String, startAddress: Int): String {
        var res = "$indent;;\n"
        var elemStartAddr = startAddress

        for (el in elements) {
            res += el.dump("$indent  ", elemStartAddr)
            elemStartAddr += el.sizeInBytes
        }

        return res
    }

    override val sizeInBytes: Int = elements.sumOf { it.sizeInBytes }
}

fun Long.toLittleEndianBytesTo(to: ByteArray, offset: Int, size: Int) {
    for (i in 0 until size) {
        to[offset + i] = (this ushr (i * 8)).toByte()
    }
}


fun Int.toLittleEndianBytes(to: ByteArray, offset: Int) {
    to[offset] = this.toByte()
    to[offset + 1] = (this ushr 8).toByte()
    to[offset + 2] = (this ushr 16).toByte()
    to[offset + 3] = (this ushr 24).toByte()
}

fun Char.toLittleEndianBytes(to: ByteArray, offset: Int, fitsLatin1: Boolean) {
    to[offset] = (this.code and 0xFF).toByte()
    if (!fitsLatin1) {
        to[offset + 1] = (this.code ushr Byte.SIZE_BITS).toByte()
    }
}