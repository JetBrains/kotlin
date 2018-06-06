/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

/**
 * The version of the metadata serialized by the compiler and deserialized by the compiler and reflection.
 * This version includes the version of the core protobuf messages (metadata.proto) as well as JVM extensions (jvm_metadata.proto).
 */
class JvmMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    // NOTE: 1.1 is incompatible with 1.0 and hence with any other version except 1.1.*
    override fun isCompatible() =
        this.major == 1 && this.minor == 1

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(1, 1, 10)

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion()
    }
}
