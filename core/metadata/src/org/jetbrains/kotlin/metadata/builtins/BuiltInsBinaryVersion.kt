/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.builtins

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.InputStream

/**
 * The version of the format in which the .kotlin_builtins file is stored. This version also includes the version
 * of the core protobuf messages (metadata.proto).
 */
class BuiltInsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible(): Boolean =
        this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = BuiltInsBinaryVersion(1, 0, 6)

        @JvmField
        val INVALID_VERSION = BuiltInsBinaryVersion()

        fun readFrom(stream: InputStream): BuiltInsBinaryVersion {
            val dataInput = DataInputStream(stream)
            return BuiltInsBinaryVersion(*(1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())
        }
    }
}
