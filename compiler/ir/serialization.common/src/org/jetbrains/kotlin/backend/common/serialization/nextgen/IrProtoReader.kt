/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl


class IrProtoReader(source: ByteArray) : ProtoReader(source) {

    fun readCoordinates(): Pair<Int, Int> {
        var start = -1
        var end = -1

        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> start = readInt32()
                    2 -> end = readInt32()
                    else -> skip(type)
                }
            }
        }

        return Pair(start, end)
    }

    fun readDataIndex(): Int {
        var result = 0

        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> result = readInt32()
                    else -> skip(type)
                }
            }
        }

        return result
    }

    fun readFileEntry(): NaiveSourceBasedFileEntryImpl {
        var name: String? = null
        val offsets = mutableListOf<Int>()

        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name = readString()
                    2 -> offsets += readInt32()
                    else -> skip(type)
                }
            }
        }

        return NaiveSourceBasedFileEntryImpl(name!!, offsets.toIntArray())
    }
}