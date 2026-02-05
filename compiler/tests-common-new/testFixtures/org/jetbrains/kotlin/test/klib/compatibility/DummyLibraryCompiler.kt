/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib.compatibility

/**
 * Represents a version of the Kotlin compiler used for testing klib compatibility.
 *
 * This class wraps [KotlinVersion] with an additional [postfix] string to support
 * version identifiers like "2.3.0-Beta1" or "2.2.255-SNAPSHOT".
 *
 * @property basicVersion The core semantic version (major.minor.patch).
 * @property postfix Additional version qualifier (e.g., "-Beta1", "-SNAPSHOT", "-RC"). Empty string for release versions.
 */
class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
    constructor(major: Int, minor: Int, patch: Int, postfix: String = "") : this(KotlinVersion(major, minor, patch), postfix)

    override fun compareTo(other: TestVersion) = basicVersion.compareTo(other.basicVersion)
    override fun equals(other: Any?) = (other as? TestVersion)?.basicVersion == basicVersion
    override fun hashCode() = basicVersion.hashCode()
    override fun toString() = basicVersion.toString() + postfix
}

/**
 * Represents the expected warning status when compiling a library with specific version combinations.
 *
 * Used in klib compatibility tests to verify that the compiler produces appropriate warnings
 * when there is a version mismatch between the library and the compiler.
 */
enum class WarningStatus {
    NO_WARNINGS,
    OLD_LIBRARY_WARNING,
    TOO_NEW_LIBRARY_WARNING
}

/**
 * Interface for compiling dummy klibs in compatibility tests.
 *
 * Implementations of this interface are used to test klib version compatibility checks across different compiler versions.
 * Each backend provides its own implementation with platform-specific compilation logic.
 */
interface DummyLibraryCompiler {
    /**
     * Compiles a dummy library with the specified version and verifies the expected warning status.
     *
     * @param libraryVersion The version to embed in the library's metadata, or `null` if version should be omitted.
     * @param compilerVersion The compiler version to simulate during compilation, or `null` if version should be omitted.
     * @param expectedWarningStatus The expected warning status after compilation based on version compatibility.
     * @param exportKlibToOlderAbiVersion If `true`, `ExportKlibToOlderAbiVersion` language feature is enabled when compiling the klib.
     */
    fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean = false,
    )
}