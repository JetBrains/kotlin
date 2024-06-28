/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion

/**
 * The version of the metadata serialized by the compiler and deserialized by the compiler and reflection.
 * This version includes the version of the core protobuf messages (metadata.proto) as well as JVM extensions (jvm_metadata.proto).
 *
 * Please note that [JvmMetadataVersion] is different compared to other [BinaryVersion]s. The version bump **DOESN'T** obey [BinaryVersion]
 * rules. Starting from Kotlin 1.4, [JvmMetadataVersion] major and minor tokens always match the compilers corresponding version tokens.
 **/
class JvmMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    fun lastSupportedVersionWithThisLanguageVersion(isStrictSemantics: Boolean): JvmMetadataVersion {
        // * Compiler of deployVersion X (INSTANCE) with LV Y (metadataVersionFromLanguageVersion)
        //   * can read metadata with version <= max(X+1, Y)
        val forwardCompatibility = if (isStrictSemantics) INSTANCE else INSTANCE_NEXT
        return if (forwardCompatibility.newerThan(this)) forwardCompatibility else this
    }

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean {
        return isCompatibleInternal(if (isStrictSemantics) INSTANCE else INSTANCE_NEXT)
    }

    fun isCompatible(metadataVersionFromLanguageVersion: JvmMetadataVersion): Boolean {
        // Special case for bootstrap: 1.8 can read 2.0
        if (major == 2 && minor == 0 && INSTANCE.major == 1 && INSTANCE.minor == 8) return true
        val limitVersion = metadataVersionFromLanguageVersion.lastSupportedVersionWithThisLanguageVersion(isStrictSemantics)
        return isCompatibleInternal(limitVersion)
    }

    private fun isCompatibleInternal(limitVersion: JvmMetadataVersion): Boolean {
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        if (major == 1 && minor == 0) return false
        // The same for 0.*
        if (major == 0) return false
        // Otherwise we just compare with the given limitVersion
        return !newerThan(limitVersion)
    }

    fun next(): JvmMetadataVersion =
        if (major == 1 && minor == 9) JvmMetadataVersion(2, 0, 0)
        else JvmMetadataVersion(major, minor + 1, 0)

    private fun newerThan(other: JvmMetadataVersion): Boolean {
        return when {
            major > other.major -> true
            major < other.major -> false
            minor > other.minor -> true
            else -> false
        }
    }

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(2, 0, 0)

        @JvmField
        val INSTANCE_NEXT = INSTANCE.next()

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion()
    }
}
