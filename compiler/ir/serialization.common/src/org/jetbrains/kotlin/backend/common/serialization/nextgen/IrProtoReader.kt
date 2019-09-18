/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.name.FqName
import java.lang.StringBuilder


abstract class IrProtoReader(source: ByteArray) : ProtoReader(source) {

    protected abstract fun readStringById(id: Int): String

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

    fun readFqName(): FqName {
        val stringBuilder = StringBuilder()

        while (hasData) {
            readField { fieldNumber, _ ->
                assert(fieldNumber == 1)
                if (stringBuilder.isNotEmpty()) stringBuilder.append('.')
                val stringId = readWithLength { readDataIndex() }
                stringBuilder.append(readStringById(stringId))
            }
        }

        return if (stringBuilder.isEmpty()) FqName.ROOT else FqName(stringBuilder.toString())
    }

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