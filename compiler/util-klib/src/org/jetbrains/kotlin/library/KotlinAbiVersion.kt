/**
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.library

fun String.parseKotlinAbiVersion(): KotlinAbiVersion {
    val values = this.split(".").map { it.toInt() }

    return when (values.size) {
        3 -> KotlinAbiVersion(values[0], values[1], values[2])
        1 -> KotlinAbiVersion(values[0])
        else -> error("Could not parse abi version: $this")
    }
}

// TODO: consider inheriting this class from BinaryVersion (but that requires a module structure refactoring.)
//  Advantages: code reuse.
//  Disadvantages: BinaryVersion is a problematic class, because it doesn't represent any logical entity in the codebase, it's just a
//  way to reuse common logic for Kotlin versions. But unfortunately, BinaryVersion is used in a lot of API definitions, which makes
//  code hard to read because it's not obvious which subclasses are supposed to be passed into a particular API.
/**
 * The version of the Kotlin IR.
 *
 * This version must be bumped when:
 * - Incompatible changes are made in `KotlinIr.proto`
 * - Incompatible changes are made in serialization/deserialization logic
 *
 * The version bump must obey [org.jetbrains.kotlin.metadata.deserialization.BinaryVersion] rules (See `BinaryVersion` KDoc)
 */
data class KotlinAbiVersion(val major: Int, val minor: Int, val patch: Int) {
    // For 1.4 compiler we switched klib abi_version to a triple,
    // but we don't break if we still encounter a single digit from 1.3.
    constructor(single: Int) : this(0, single, 0)

    fun isCompatible(): Boolean = isCompatibleTo(CURRENT)

    private fun isCompatibleTo(ourVersion: KotlinAbiVersion): Boolean {
        return if (this.isAtLeast(FIRST_WITH_EXPERIMENTAL_BACKWARD_COMPATIBILITY))
            this.isAtMost(ourVersion)
        else
            this == ourVersion
    }

    fun isAtLeast(version: KotlinAbiVersion): Boolean =
        isAtLeast(version.major, version.minor, version.patch)

    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean {
        if (this.major > major) return true
        if (this.major < major) return false

        if (this.minor > minor) return true
        if (this.minor < minor) return false

        return this.patch >= patch
    }

    fun isAtMost(version: KotlinAbiVersion): Boolean =
        isAtMost(version.major, version.minor, version.patch)

    fun isAtMost(major: Int, minor: Int, patch: Int): Boolean {
        if (this.major < major) return true
        if (this.major > major) return false

        if (this.minor < minor) return true
        if (this.minor > minor) return false

        return this.patch <= patch
    }

    override fun toString() = "$major.$minor.$patch"

    companion object {
        /**
         * See: [KotlinAbiVersion bump history](compiler/util-klib/KotlinAbiVersionBumpHistory.md)
         *
         * Since the release of 2.2.0, the ABI version is aligned with the Kotlin version.
         */
        val CURRENT = KotlinAbiVersion(2, 3, 0)

        /**
         * Versions before 1.4.1 were the active development phase.
         * Starting with 1.4.1 we are trying to maintain experimental backward compatibility.
         */
        private val FIRST_WITH_EXPERIMENTAL_BACKWARD_COMPATIBILITY = KotlinAbiVersion(1, 4, 1)
    }
}
