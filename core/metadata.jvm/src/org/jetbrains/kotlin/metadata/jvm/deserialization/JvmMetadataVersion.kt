/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

/**
 * The version of the metadata serialized by the compiler and deserialized by the compiler and reflection.
 * This version includes the version of the core protobuf messages (metadata.proto) as well as JVM extensions (jvm_metadata.proto).
 */
class JvmMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean {
        return isCompatibleInternal(INSTANCE_NEXT)
    }

    fun isCompatible(metadataVersionFromLanguageVersion: JvmMetadataVersion): Boolean {
        // Special case for bootstrap: 1.8 can read 2.0
        if (major == 2 && minor == 0 && INSTANCE.major == 1 && INSTANCE.minor == 8) return true
        // * Compiler of deployVersion X (INSTANCE) with LV Y (metadataVersionFromLanguageVersion)
        //   * can read metadata with version <= max(X+1, Y)
        val forwardCompatibility = if (isStrictSemantics) INSTANCE else INSTANCE_NEXT
        val limitVersion = maxOf(forwardCompatibility, metadataVersionFromLanguageVersion)
        return isCompatibleInternal(limitVersion)
    }

    private fun isCompatibleInternal(limitVersion: JvmMetadataVersion): Boolean {
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        if (major == 1 && minor == 0) return false
        // The same for 0.*
        if (major == 0) return false
        // Otherwise we just compare with the given limitVersion
        return this <= limitVersion
    }

    fun next(): JvmMetadataVersion {
        if (minor < 9 || major > 1) return JvmMetadataVersion(major, minor + 1, 0)
        return JvmMetadataVersion(2, 0, 0)
    }

    fun prev(): JvmMetadataVersion {
        if (minor > 0) return JvmMetadataVersion(major, minor - 1, 0)
        return JvmMetadataVersion(major - 1, 9, 0)
    }

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(1, 8, 0)

        private val INSTANCE_NEXT = INSTANCE.next()

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion()
    }
}
