/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.deserialization

/**
 * Subclasses of this class are used to identify different versions of the binary output of the compiler and their compatibility guarantees.
 * - Major version should be increased only when the new binary format is neither forward- nor backward compatible.
 *   This shouldn't really ever happen at all.
 * - Minor version should be increased when the new format is backward compatible,
 *   i.e. the new compiler can process old data, but the old compiler will not be able to process new data.
 * - Patch version can be increased freely and is only supposed to be used for debugging. Increase the patch version when you
 *   make a change to binaries which is both forward- and backward compatible.
 */
abstract class BinaryVersion(vararg val numbers: Int) {
    val major: Int = numbers.getOrNull(0) ?: UNKNOWN
    val minor: Int = numbers.getOrNull(1) ?: UNKNOWN
    val patch: Int = numbers.getOrNull(2) ?: UNKNOWN
    val rest: List<Int> = if (numbers.size > 3) numbers.asList().subList(3, numbers.size).toList() else emptyList()

    abstract fun isCompatible(): Boolean

    fun toArray(): IntArray = numbers

    /**
     * Returns true if this version of some format loaded from some binaries is compatible
     * to the expected version of that format in the current compiler.
     *
     * @param ourVersion the version of this format in the current compiler
     */
    protected fun isCompatibleTo(ourVersion: BinaryVersion): Boolean {
        return if (major == 0) ourVersion.major == 0 && minor == ourVersion.minor
        else major == ourVersion.major && minor <= ourVersion.minor
    }

    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean {
        if (this.major > major) return true
        if (this.major < major) return false

        if (this.minor > minor) return true
        if (this.minor < minor) return false

        return this.patch >= patch
    }

    override fun toString(): String {
        val versions = toArray().takeWhile { it != UNKNOWN }
        return if (versions.isEmpty()) "unknown" else versions.joinToString(".")
    }

    override fun equals(other: Any?) =
            other != null &&
            this::class.java == other::class.java &&
            major == (other as BinaryVersion).major && minor == other.minor && patch == other.patch && rest == other.rest

    override fun hashCode(): Int{
        var result = major
        result += 31 * result + minor
        result += 31 * result + patch
        result += 31 * result + rest.hashCode()
        return result
    }

    companion object {
        private val UNKNOWN = -1
    }
}
