/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.library.impl.toArray
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrMemoryStringWriter
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader

class JsIrFileMetadata(val exportedNames: List<String>) : IrFileSerializer.FileBackendSpecificMetadata {
    override fun toByteArray(): ByteArray {
        return IrMemoryStringWriter(exportedNames).writeIntoMemory()
    }

    companion object {
        fun fromByteArray(data: ByteArray): JsIrFileMetadata {
            return JsIrFileMetadata(
                exportedNames = IrArrayMemoryReader(data).toArray().map(WobblyTF8::decode)
            )
        }
    }
}
