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

data class BinaryVersion private constructor(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val rest: List<Int> = listOf()
) {
    fun toArray(): IntArray =
            intArrayOf(major, minor, patch, *rest.toIntArray())

    /**
     * Returns true if this version of some format loaded from some binaries is compatible
     * to the expected version of that format in the current compiler.
     *
     * @param ourVersion the version of this format in the current compiler
     */
    fun isCompatibleTo(ourVersion: BinaryVersion): Boolean {
        return if (major == 0) ourVersion.major == 0 && minor == ourVersion.minor
        else major == ourVersion.major && minor <= ourVersion.minor
    }

    override fun toString(): String {
        val versions = toArray().takeWhile { it != UNKNOWN }
        return if (versions.isEmpty()) "unknown" else versions.joinToString(".")
    }

    companion object {
        private val UNKNOWN = -1

        @JvmStatic
        fun create(version: IntArray): BinaryVersion {
            return BinaryVersion(
                    major = version.getOrNull(0) ?: UNKNOWN,
                    minor = version.getOrNull(1) ?: UNKNOWN,
                    patch = version.getOrNull(2) ?: UNKNOWN,
                    rest = if (version.size > 3) version.asList().subList(3, version.size).toList() else listOf()
            )
        }

        @JvmStatic
        fun create(major: Int, minor: Int, patch: Int) = BinaryVersion(major, minor, patch)
    }
}
