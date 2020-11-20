/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.wasm.ir.WasmSymbol

// Representation of constant data in Wasm memory

sealed class ConstantDataElement {
    abstract val sizeInBytes: Int
    abstract fun dump(indent: String = "", startAddress: Int = 0): String
    abstract fun toBytes(): ByteArray
}

private fun addressToString(address: Int): String =
    address.toString().padEnd(6, ' ')

class ConstantDataIntField(val name: String, val value: WasmSymbol<Int>) : ConstantDataElement() {
    constructor(name: String, value: Int) : this(name, WasmSymbol(value))

    override fun toBytes(): ByteArray = value.owner.toLittleEndianBytes()

    override fun dump(indent: String, startAddress: Int): String {
        return "${addressToString(startAddress)}: $indent i32   : ${value.owner}    ;; $name\n"
    }

    override val sizeInBytes: Int = 4
}

class ConstantDataIntArray(val name: String, val value: List<WasmSymbol<Int>>) : ConstantDataElement() {
    override fun toBytes(): ByteArray {
        return value.fold(byteArrayOf()) { acc, el -> acc + el.owner.toLittleEndianBytes() }
    }

    override fun dump(indent: String, startAddress: Int): String {
        if (value.isEmpty()) return ""
        return "${addressToString(startAddress)}: $indent i32[] : ${value.map { it.owner }.toIntArray().contentToString()}   ;; $name\n"
    }

    override val sizeInBytes: Int = value.size * 4
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

fun Int.toLittleEndianBytes(): ByteArray {
    return ByteArray(4) {
        (this ushr (it * 8)).toByte()
    }
}