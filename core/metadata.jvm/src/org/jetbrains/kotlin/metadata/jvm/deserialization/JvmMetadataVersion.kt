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
class JvmMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    override fun isCompatible(): Boolean =
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        (major != 1 || minor != 0) &&
                if (isStrictSemantics) {
                    isCompatibleTo(INSTANCE)
                } else {
                    // In Kotlin 1.4, JVM metadata version is going to be advanced to 1.4.0.
                    // Kotlin 1.3 is able to read metadata of versions up to Kotlin 1.4 (unless the version has strict semantics).
                    major == 1 && minor <= 4
                }

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(1, 1, 15)

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion()
    }
}
