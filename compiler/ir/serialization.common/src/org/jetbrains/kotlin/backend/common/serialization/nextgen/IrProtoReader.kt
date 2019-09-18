/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl


class IrProtoReader(source: ByteArray): ProtoReader(source) {

    fun readFileEntry(): NaiveSourceBasedFileEntryImpl {
        var name: String? = null
        val offsets = mutableListOf<Int>()

        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        name = readString()
                    }
                    2 -> {
                        offsets += readInt32()
                    }
                }
            }
        }

        return NaiveSourceBasedFileEntryImpl(name!!, offsets.toIntArray())
    }

}