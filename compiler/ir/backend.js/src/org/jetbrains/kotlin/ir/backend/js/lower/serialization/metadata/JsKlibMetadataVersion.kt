/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.InputStream

class JsKlibMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible(): Boolean =
        this.isCompatibleTo(INSTANCE)

    fun toInteger() = (patch shl 16) + (minOf(minor, 255) shl 8) + minOf(major, 255)

    companion object {
        @JvmField
        val INSTANCE = JsKlibMetadataVersion(1, 2, 6)

        @JvmField
        val INVALID_VERSION = JsKlibMetadataVersion()

        fun fromInteger(version: Int): JsKlibMetadataVersion =
            JsKlibMetadataVersion(version and 255, (version shr 8) and 255, version shr 16)

        fun readFrom(stream: InputStream): JsKlibMetadataVersion {
            val dataInput = DataInputStream(stream)
            val size = dataInput.readInt()

            // We assume here that the version will always have 3 components. This is needed to prevent reading an unpredictable amount
            // of integers from old .kjsm files (pre-1.1) because they did not have the version in the beginning
            if (size != INSTANCE.toArray().size) return INVALID_VERSION

            return JsKlibMetadataVersion(*(1..size).map { dataInput.readInt() }.toIntArray())
        }
    }
}