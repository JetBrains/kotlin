/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.library.*
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

        // TODO (KT-83853): Find a reliable way to detect dev compiler versions.
        val isDevVersion: Boolean = "-dev-" in rawVersion || rawVersion.endsWith("-SNAPSHOT")

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

    protected open fun libraryVersion(library: KotlinLibrary): Version? =
        library.getComponent(JarManifestComponent.Kind)?.jarManifest?.let { jarManifest ->
            Version.parseVersion(jarManifest.mainAttributes.getValue(KLIB_JAR_LIBRARY_VERSION))
        }

    fun check(
        libraries: Collection<KotlinLibrary>,
        messageCollector: MessageCollector,
        klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel,
    ) {
        val compilerVersion = Version.parseVersion(getRawCompilerVersion()) ?: return
        val isLatestKlibAbiCompatibilityLevel = klibAbiCompatibilityLevel == KlibAbiCompatibilityLevel.LATEST_STABLE

        // It might happen that the compiler has already got a new major version (N.M+1.0), but there is still the old bootstrap compiler
        // version (N.M,*) used to compile stdlib & kotlin-test libraries. As a result, these libraries still have `abi_version=N.M.0`
        // in their manifest files. And the compatibility check, if it were applied, would fail.
        val useRelaxedCompatibilityCheckForDevCompilerVersion = isLatestKlibAbiCompatibilityLevel && compilerVersion.isDevVersion

        for (library in libraries) {
            val checkedLibrary = library.toCheckedLibrary() ?: continue

            val libraryVersion = libraryVersion(library)

            val libraryAbiVersion = library.versions.abiVersion ?: continue

            val isLibraryAbiCompatible = libraryAbiVersion.isCompatibleWithAbiLevel(klibAbiCompatibilityLevel) ||
                    useRelaxedCompatibilityCheckForDevCompilerVersion && libraryAbiVersion.isCompatibleWithAbiLevel(klibAbiCompatibilityLevel.previous())

            val errorMessage = when {
                !isLibraryAbiCompatible ->
                    message(
                        rootCause = "The ${checkedLibrary.platformDisplayName} ${checkedLibrary.libraryDisplayName} library has the ABI version (${library.versions.abiVersion}) that is not compatible with the compiler's current ABI compatibility level ($klibAbiCompatibilityLevel).",
                        libraryName = checkedLibrary.libraryDisplayName,
                        versionKind = "ABI version",
                        minAcceptedVersion = "$klibAbiCompatibilityLevel.0",
                        maxAcceptedVersion = "$klibAbiCompatibilityLevel.${KotlinVersion.MAX_COMPONENT_VALUE}"
                    )
                isLatestKlibAbiCompatibilityLevel && libraryVersion != null && libraryVersion < compilerVersion ->
                    message(
                        rootCause = "The ${checkedLibrary.platformDisplayName} ${checkedLibrary.libraryDisplayName} library has an older version ($libraryVersion) than the compiler ($compilerVersion). Such a configuration is not supported.",
                        libraryName = checkedLibrary.libraryDisplayName,
                        versionKind = "version",
                        minAcceptedVersion = compilerVersion.toComparableVersionString(),
                        maxAcceptedVersion = "${compilerVersion.toLanguageVersionString()}.${KotlinVersion.MAX_COMPONENT_VALUE}"
                    )
                else -> continue
            }

            messageCollector.report(CompilerMessageSeverity.ERROR, errorMessage)
        }
    }

    private fun message(
        rootCause: String,
        libraryName: String,
        versionKind: String,
        minAcceptedVersion: String,
        maxAcceptedVersion: String
    ): String =
        "$rootCause\nPlease, make sure that the $libraryName library $versionKind is in the range " +
                "[$minAcceptedVersion .. $maxAcceptedVersion]. Adjust your project's settings if necessary."

    private fun KotlinAbiVersion.isCompatibleWithAbiLevel(klibAbiCompatibilityLevel: KlibAbiCompatibilityLevel?): Boolean {
        if (klibAbiCompatibilityLevel == null) return false
        return klibAbiCompatibilityLevel.major == this.major && klibAbiCompatibilityLevel.minor == this.minor
    }

    private fun KotlinLibrary.isCompatible(compilerAbiCompatibilityLevel: KlibAbiCompatibilityLevel): Boolean {
        val libraryAbiVersion = this.versions.abiVersion
        return if (libraryAbiVersion == null) true
        else compilerAbiCompatibilityLevel.major == libraryAbiVersion.major && compilerAbiCompatibilityLevel.minor == libraryAbiVersion.minor
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
