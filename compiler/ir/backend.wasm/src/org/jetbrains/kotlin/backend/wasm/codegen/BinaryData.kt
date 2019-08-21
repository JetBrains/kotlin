/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

// Representation of constant data in wasm memory

sealed class BinaryDataElement {
    abstract val sizeInBytes: Int
    abstract fun dump(indent: String = "", startAddress: Int = 0): String
    abstract fun toBytes(): ByteArray
}

private fun addressToString(address: Int): String =
    address.toString().padEnd(6, ' ')

class BinaryDataIntField(val name: String, val value: Int) : BinaryDataElement() {
    override fun toBytes(): ByteArray = value.toLittleEndianBytes()

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : $value    ;; $name\n"
    }

    override val sizeInBytes: Int = 4
}

class BinaryDataIntArray(val name: String, val value: List<Int>) : BinaryDataElement() {
    override fun toBytes(): ByteArray {
        return value.fold(byteArrayOf()) { acc, el -> acc + el.toLittleEndianBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i32[] : ${value.toIntArray().contentToString()}   ;; $name\n"
    }

    override val sizeInBytes: Int = value.size * 4
}

class BinaryDataStruct(val name: String, val elements: List<BinaryDataElement>) : BinaryDataElement() {
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

fun Int.toLittleEndianBytes(): ByteArray {
    return ByteArray(4) {
        (this ushr (it * 8)).toByte()
    }
}