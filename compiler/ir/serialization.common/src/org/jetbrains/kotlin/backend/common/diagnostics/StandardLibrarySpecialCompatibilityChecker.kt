/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.KotlinLibraryImpl
import org.jetbrains.kotlin.library.impl.javaFile
import java.io.IOException
import java.util.jar.Manifest

/** See KT-68322 for details. */
abstract class StandardLibrarySpecialCompatibilityChecker {
    protected class Version(private val comparableVersion: MavenComparableVersion, private val fullVersion: String) : Comparable<Version> {
        override fun compareTo(other: Version) = comparableVersion.compareTo(other.comparableVersion)
        override fun equals(other: Any?) = (other as? Version)?.comparableVersion == comparableVersion
        override fun hashCode() = comparableVersion.hashCode()
        override fun toString() = fullVersion

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

                return Version(comparableVersion, fullVersion = rawVersion)
            }
        }
    }

    fun check(libraries: Collection<KotlinLibrary>, messageCollector: MessageCollector) {
        val compilerVersion = Version.parseVersion(getRawCompilerVersion()) ?: return

        for (library in libraries) {
            if (isStdlib(library)) {
                val jarManifest = library.getJarManifest() ?: return
                val stdlibVersion = Version.parseVersion(jarManifest.mainAttributes.getValue(KLIB_JAR_LIBRARY_VERSION)) ?: return

                val messageToReport = getMessageToReport(compilerVersion, stdlibVersion)
                if (messageToReport != null) {
                    messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, messageToReport)
                }

                return
            }
        }
    }

    private fun KotlinLibrary.getJarManifest(): Manifest? {
        val libraryAccess = (this as KotlinLibraryImpl).base.access
        val jarManifestFile = libraryAccess.inPlace { it.libFile.child(KLIB_JAR_MANIFEST_FILE).javaFile() }
        if (!jarManifestFile.isFile) return null

        return try {
            jarManifestFile.inputStream().use { Manifest(it) }
        } catch (e: IOException) {
            null
        }
    }

    private fun getRawCompilerVersion(): String? {
        return customCompilerVersionForTest?.let { return it.version } ?: KotlinCompilerVersion.getVersion()
    }

    protected abstract fun isStdlib(library: KotlinLibrary): Boolean
    protected abstract fun getMessageToReport(compilerVersion: Version, stdlibVersion: Version): String?

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
