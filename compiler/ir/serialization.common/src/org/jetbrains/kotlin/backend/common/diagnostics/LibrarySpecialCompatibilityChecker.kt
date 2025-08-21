/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.KotlinLibraryImpl
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.jar.Manifest

/** See KT-68322 for details. */
abstract class LibrarySpecialCompatibilityChecker {
    protected class Version(
        private val comparableVersion: MavenComparableVersion,
        private val languageVersion: LanguageVersion,
        private val rawVersion: String
    ) : Comparable<Version> {
        override fun compareTo(other: Version) = comparableVersion.compareTo(other.comparableVersion)
        override fun equals(other: Any?) = (other as? Version)?.comparableVersion == comparableVersion
        override fun hashCode() = comparableVersion.hashCode()

        fun hasSameLanguageVersion(other: Version) = languageVersion == other.languageVersion

        override fun toString() = rawVersion
        fun toComparableVersionString() = comparableVersion.toString()
        fun toLanguageVersionString() = languageVersion.toString()

        companion object {
            fun parseVersion(rawVersion: String?): Version? {
                if (rawVersion == null) return null

                val comparableVersion = try {
                    // We use `substringBefore('-')` to cut off irrelevant part of the version string.
                    // Ex: 2.0.255-SNAPSHOT -> 2.0.255, 2.0.20-dev-12345 -> 2.0.20
                    MavenComparableVersion(rawVersion.substringBefore('-'))
                } catch (e: Exception) {
                    return null
                }

                val languageVersion = LanguageVersion.fromFullVersionString(rawVersion)
                    ?: return null

                return Version(comparableVersion, languageVersion, rawVersion)
            }
        }
    }

    fun check(libraries: Collection<KotlinLibrary>, messageCollector: MessageCollector) {
        val compilerVersion = Version.parseVersion(getRawCompilerVersion()) ?: return

        for (library in libraries) {
            if (shouldCheckLibrary(library)) {
                val jarManifest = library.getJarManifest() ?: continue
                val libraryVersion = Version.parseVersion(jarManifest.mainAttributes.getValue(KLIB_JAR_LIBRARY_VERSION)) ?: continue

                val messageToReport = getMessageToReport(compilerVersion, libraryVersion, library)
                if (messageToReport != null) {
                    messageCollector.report(CompilerMessageSeverity.ERROR, messageToReport)
                }
            }
        }
    }

    private fun KotlinLibrary.getJarManifest(): Manifest? =
        (this as KotlinLibraryImpl).base.access.inPlace { layout ->
            val jarManifestFile = layout.libFile.child(KLIB_JAR_MANIFEST_FILE)
            if (!jarManifestFile.isFile) return@inPlace null

            try {
                ByteArrayInputStream(jarManifestFile.readBytes()).use { Manifest(it) }
            } catch (_: IOException) {
                null
            }
        }

    private fun getRawCompilerVersion(): String? {
        return customCompilerVersionForTest?.let { return it.version } ?: KotlinCompilerVersion.getVersion()
    }

    protected abstract fun shouldCheckLibrary(library: KotlinLibrary): Boolean
    protected abstract fun getMessageToReport(compilerVersion: Version, libraryVersion: Version, library: KotlinLibrary): String?

    companion object {
        private class CustomCompilerVersionForTest(val version: String?)

        private var customCompilerVersionForTest: CustomCompilerVersionForTest? = null

        @Deprecated("Only for test purposes, use with care!")
        fun setUpCustomCompilerVersionForTest(compilerVersion: String?) {
            customCompilerVersionForTest = CustomCompilerVersionForTest(compilerVersion)
        }

        @Deprecated("Only for test purposes, use with care!")
        fun resetUpCustomCompilerVersionForTest() {
            customCompilerVersionForTest = null
        }

        const val KLIB_JAR_MANIFEST_FILE = "META-INF/MANIFEST.MF"
        const val KLIB_JAR_LIBRARY_VERSION = "Implementation-Version"
    }
}
