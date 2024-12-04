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

class ConstantDataCharField(val name: String, val value: WasmSymbol<Char>) : ConstantDataElement() {
    constructor(name: String, value: Char) : this(name, WasmSymbol(value))

    override fun toBytes(): ByteArray = value.owner.toLittleEndianBytes()

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : ${value.owner}    ;; $name\n"
    }

    override val sizeInBytes: Int = CHAR_SIZE_BYTES
}

class ConstantDataIntField(val name: String, val value: WasmSymbol<Int>) : ConstantDataElement() {
    constructor(name: String, value: Int) : this(name, WasmSymbol(value))

    override fun toBytes(): ByteArray = value.owner.toLittleEndianBytes()

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : ${value.owner}    ;; $name\n"
    }

    override val sizeInBytes: Int = INT_SIZE_BYTES
}

class ConstantDataIntegerArray(val name: String, val value: List<Long>, val integerSize: Int) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        val array = ByteArray(value.size * integerSize)
        repeat(value.size) { i ->
            value[i].toLittleEndianBytesTo(array, i * integerSize, integerSize)
        }
        return array
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i${integerSize * 8}[] : ${toBytes().contentToString()}   ;; $name\n"
    }

    override val sizeInBytes: Int = value.size * integerSize
}

class ConstantDataIntArray(val name: String, val value: List<WasmSymbol<Int>>) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        return value.fold(byteArrayOf()) { acc, el -> acc + el.owner.toLittleEndianBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i32[] : ${value.map { it.owner }.toIntArray().contentToString()}   ;; $name\n"
    }

    override val sizeInBytes: Int = value.size * INT_SIZE_BYTES
}

class ConstantDataCharArray(val name: String, val value: List<WasmSymbol<Char>>) : ConstantDataElement() {
    constructor(name: String, value: CharArray) : this(name, value.map { WasmSymbol(it) })

    override fun toBytes(): ByteArray {
        return value
            .map { it.owner.toLittleEndianBytes() }
            .fold(byteArrayOf(), ByteArray::plus)
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i16[] : ${value.map { it.owner }.toCharArray().contentToString()}   ;; $name\n"
    }

    override val sizeInBytes: Int = value.size * CHAR_SIZE_BYTES
}

class ConstantDataStruct(val name: String, val elements: List<ConstantDataElement>) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        return elements.fold(byteArrayOf()) { acc, el -> acc + el.toBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        var res = "$indent;; $name\n"
        var elemStartAddr = startAddress

        for (el in elements) {
            res += el.dump("$indent  ", elemStartAddr)
            elemStartAddr += el.sizeInBytes
        }

        return res
    }

    override val sizeInBytes: Int = elements.map { it.sizeInBytes }.sum()
}

fun Long.toLittleEndianBytesTo(to: ByteArray, offset: Int, size: Int) {
    for (i in 0 until size) {
        to[offset + i] = (this ushr (i * 8)).toByte()
    }
}


fun Int.toLittleEndianBytes(): ByteArray {
    return ByteArray(4) {
        (this ushr (it * 8)).toByte()
    }
}

fun Char.toLittleEndianBytes(): ByteArray {
    return byteArrayOf((this.code and 0xFF).toByte(), (this.code ushr Byte.SIZE_BITS).toByte())
}