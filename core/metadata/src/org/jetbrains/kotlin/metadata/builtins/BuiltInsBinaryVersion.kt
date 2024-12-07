/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.builtins

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.InputStream

/**
 * The version of the format in which the `.kotlin_builtins` (`builtins.proto`) file is stored. This version also includes the version
 * of the core protobuf messages (`metadata.proto`).
 *
 * This version must be bumped when:
 * - Incompatible changes are made in `builtins.proto`
 * - Incompatible changes are made in `metadata.proto`
 * - Incompatible changes are made in builtins serialization/deserialization logic
 *
 * The version bump must obey [org.jetbrains.kotlin.metadata.deserialization.BinaryVersion] rules (See `BinaryVersion` KDoc).
 */
class BuiltInsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatibleWithCurrentCompilerVersion(): Boolean =
        this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = BuiltInsBinaryVersion(1, 0, 7)

        @JvmField
        val INVALID_VERSION = BuiltInsBinaryVersion()

        fun readFrom(stream: InputStream): BuiltInsBinaryVersion {
            val dataInput = DataInputStream(stream)
            return BuiltInsBinaryVersion(*(1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())
        }
    }
}
