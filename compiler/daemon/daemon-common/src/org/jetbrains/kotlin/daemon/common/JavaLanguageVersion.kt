/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.Serializable

/**
 * Represents a Java major version (e.g. 8, 11, 17, 21) parsed from a Java version string
 * such as those produced by the `java.specification.version` or `java.version` system properties.
 */
interface JavaLanguageVersion : Comparable<JavaLanguageVersion>, Serializable {

    val version: Int

    override fun compareTo(other: JavaLanguageVersion): Int = version.compareTo(other.version)

    companion object {
        fun of(languageVersion: Int): JavaLanguageVersion {
            require(languageVersion > 0) { "Java language version must be positive, got $languageVersion" }
            return JavaLanguageVersionImpl(languageVersion)
        }

        fun parse(versionString: String?): JavaLanguageVersion {
            if (versionString == null) return UnknownJavaLanguageVersion
            val versionNumber = parseMajorVersion(versionString) ?: return UnknownJavaLanguageVersion
            return JavaLanguageVersionImpl(versionNumber)
        }

        private fun parseMajorVersion(version: String): Int? {
            val trimmed = version.trim()
            // Legacy format: 1.X[.Y[_Z]] → X (e.g. "1.8" or "1.8.0_292" → 8)
            if (trimmed.startsWith("1.")) {
                return trimmed.drop(2).takeWhile(Char::isDigit).toIntOrNull()
            }
            // Modern format: X[.Y.Z][-qualifier][+build] → X (e.g. "17" or "17.0.1+12-LTS" → 17)
            return trimmed.takeWhile(Char::isDigit).toIntOrNull()
        }
    }
}

private data object UnknownJavaLanguageVersion : JavaLanguageVersion {

    override val version: Int = -1

    @Suppress("unused")
    private const val serialVersionUID: Long = 1L

    @Suppress("unused")
    private fun readResolve(): Any = UnknownJavaLanguageVersion
}

private data class JavaLanguageVersionImpl(override val version: Int) : JavaLanguageVersion {

    override fun toString(): String {
        if (version < 5) {
            return "1.${version}"
        }
        return "$version"
    }

    companion object {
        @Suppress("unused")
        private const val serialVersionUID: Long = 1L
    }
}
