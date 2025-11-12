/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.KotlinLibrary
import java.io.ByteArrayInputStream
import java.util.jar.Manifest
import org.jetbrains.kotlin.konan.file.File as KlibFile

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
                } catch (_: Exception) {
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
            val checkedLibrary = library.toCheckedLibrary() ?: continue

            val jarManifest = library.getComponent(JarManifestComponent.Kind)?.jarManifest ?: continue
            val libraryVersion = Version.parseVersion(jarManifest.mainAttributes.getValue(KLIB_JAR_LIBRARY_VERSION)) ?: continue

            val rootCause = when {
                libraryVersion < compilerVersion ->
                    "The ${checkedLibrary.platformDisplayName} ${checkedLibrary.libraryDisplayName} library has an older version ($libraryVersion) than the compiler ($compilerVersion). Such a configuration is not supported."

                !libraryVersion.hasSameLanguageVersion(compilerVersion) ->
                    "The ${checkedLibrary.platformDisplayName} ${checkedLibrary.libraryDisplayName} library has a more recent version ($libraryVersion) than the compiler supports. The compiler version is $compilerVersion."

                else -> continue
            }

            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "$rootCause\nPlease, make sure that the ${checkedLibrary.libraryDisplayName} library has the version in the range " +
                        "[${compilerVersion.toComparableVersionString()} .. ${compilerVersion.toLanguageVersionString()}.${KotlinVersion.MAX_COMPONENT_VALUE}]. " +
                        "Adjust your project's settings if necessary."
            )
        }
    }

    private fun getRawCompilerVersion(): String? {
        return customCompilerVersionForTest?.let { return it.version } ?: KotlinCompilerVersion.getVersion()
    }

    protected class CheckedLibrary(val libraryDisplayName: String, val platformDisplayName: String)

    protected abstract fun KotlinLibrary.toCheckedLibrary(): CheckedLibrary?

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

private class JarManifestComponent(
    private val layoutReader: KlibLayoutReader<JarManifestComponentLayout>
) : KlibComponent {
    val jarManifest: Manifest?
        get() = layoutReader.readInPlaceOrFallback(null) {
            ByteArrayInputStream(it.jarManifestFile.readBytes()).use(::Manifest)
        }

    object Kind : KlibComponent.Kind<JarManifestComponent, JarManifestComponentLayout> {
        override fun createLayout(root: KlibFile) = JarManifestComponentLayout(root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<JarManifestComponentLayout>) =
            if (layoutReader.readInPlaceOrFallback(false) { it.jarManifestFile.isFile }) JarManifestComponent(layoutReader) else null
    }
}

private class JarManifestComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    val jarManifestFile: KlibFile
        get() = root.child(KLIB_JAR_MANIFEST_FILE)
}
