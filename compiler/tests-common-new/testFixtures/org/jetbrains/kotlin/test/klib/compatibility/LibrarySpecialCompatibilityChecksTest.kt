/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib.compatibility

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.File
import java.util.*

@Execution(ExecutionMode.SAME_THREAD)
abstract class LibrarySpecialCompatibilityChecksTest : DummyLibraryCompiler {
    /**
     * Since the ABI version is bumped after the language version, it may happen that after bumping the language version
     * [KotlinAbiVersion.Companion.CURRENT] != [LanguageVersion.LATEST_STABLE]. This can cause issues in library compatibility tests: for example,
     * when exporting a klib to the previous ABI version, we may use a 2.X compiler while the previous ABI version is 2.(X − 2).
     * Since this is only a temporary situation (the ABI version is usually bumped shortly after the language version),
     * we simply ignore these tests when this happens.
     */
    @BeforeEach
    fun assumeAbiAndLanguageAligned() {
        Assumptions.assumeTrue(abiAndLanguageAreAligned(), "ABI and language basic versions are not aligned")
    }

    @TempDir
    private lateinit var tmpdir: File

    @Test
    fun testSameBasicCompilerVersion() {
        for (versionsWithSameBasicVersion in SORTED_TEST_COMPILER_VERSION_GROUPS) {
            for (libraryVersion in versionsWithSameBasicVersion) {
                for (compilerVersion in versionsWithSameBasicVersion) {
                    compileDummyLibrary(
                        libraryVersion = libraryVersion,
                        compilerVersion = compilerVersion,
                        expectedWarningStatus = WarningStatus.NO_WARNINGS
                    )
                }
            }
        }
    }

    @Test
    fun testNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                libraryVersion = currentVersion,
                compilerVersion = nextVersion,
                expectedWarningStatus = WarningStatus.OLD_LIBRARY_WARNING
            )
        }
    }

    @Test
    fun testOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                libraryVersion = nextVersion,
                compilerVersion = currentVersion,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.TOO_NEW_LIBRARY_WARNING
            )
        }
    }

    @Test
    fun testEitherVersionIsMissing() {
        listOf(
            TestVersion(2, 0, 0) to null,
            null to TestVersion(2, 0, 0),
        ).forEach { (libraryVersion, compilerVersion) ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    protected abstract fun abiAndLanguageAreAligned(): Boolean

    private inline fun testCurrentAndNextBasicVersions(block: (currentVersion: TestVersion, nextVersion: TestVersion) -> Unit) {
        for (i in 0..SORTED_TEST_COMPILER_VERSION_GROUPS.size - 2) {
            val versionsWithSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i]
            val versionsWithNextSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i + 1]

            for (currentVersion in versionsWithSameBasicVersion) {
                for (nextVersion in versionsWithNextSameBasicVersion) {
                    block(currentVersion, nextVersion)
                }
            }
        }
    }

    private fun haveSameLanguageVersion(a: TestVersion, b: TestVersion): Boolean =
        a.basicVersion.major == b.basicVersion.major && a.basicVersion.minor == b.basicVersion.minor

    protected abstract val originalLibraryPath: String
    protected open fun additionalLibraries(): List<String> = listOf()

    protected fun createDir(name: String): File = tmpdir.resolve(name).apply { mkdirs() }
    protected fun createFile(name: String): File = tmpdir.resolve(name).apply { parentFile.mkdirs() }

    companion object {
        private val currentKotlinVersion = KotlinVersion.CURRENT

        private val VERSIONS = listOf(
            0 to "",
            0 to "-dev-1234",
            0 to "-dev-4321",
            0 to "-Beta1",
            0 to "-Beta2",
            20 to "",
            20 to "-Beta1",
            20 to "-Beta2",
            255 to "-SNAPSHOT",
        )

        val SORTED_TEST_COMPILER_VERSION_GROUPS: List<Collection<TestVersion>> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor, patch, postfix) }
                .groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        val SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS: List<TestVersion> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor - 1, patch, postfix) }
    }
}
