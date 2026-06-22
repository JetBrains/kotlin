/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib.compatibility

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_LIBRARY_VERSION
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.io.readProperties
import org.jetbrains.kotlin.io.unzipTo
import org.jetbrains.kotlin.io.writeProperties
import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.*
import java.util.jar.Manifest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

data class CompilerInvocationContext(
    val sourceFile: Path,
    val outputDir: Path,
    val moduleName: String,
    val fakeLibraryPath: Path,
    val additionalLibraries: List<Path>,
    val exportKlibToOlderAbiVersion: Boolean,
    val messageCollector: MessageCollectorImpl,
    val expectedExitCode: ExitCode,
)

@Isolated
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
        Assumptions.assumeTrue(
            LanguageVersion.LATEST_STABLE.major == KotlinAbiVersion.CURRENT.major && LanguageVersion.LATEST_STABLE.minor == KotlinAbiVersion.CURRENT.minor,
            "ABI and language basic versions are not aligned"
        )
    }

    @TempDir
    private lateinit var tmpdir: Path

    protected lateinit var testName: String

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testName = testInfo.testMethod.get().name
    }

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
        ).forEach { [libraryVersion, compilerVersion] ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

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

    protected abstract val originalLibraryPath: Path
    protected open fun additionalLibraries(): List<Path> = listOf()

    protected abstract fun runCompiler(context: CompilerInvocationContext)

    override fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean,
    ) {
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = false, expectedWarningStatus, exportKlibToOlderAbiVersion)
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = true, expectedWarningStatus, exportKlibToOlderAbiVersion)
    }

    private fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        isZipped: Boolean,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean,
    ) {
        val sourcesDir = createDir("sources")
        val outputDir = createDir("build")

        val sourceFile = sourcesDir.resolve("file.kt").apply { writeText("fun foo() = 42\n") }
        val moduleName = testName

        val messageCollector = MessageCollectorImpl()

        withCustomCompilerVersion(compilerVersion) {
            val fakeLibrary = if (isZipped)
                createFakeZippedLibraryWithSpecificVersion(libraryVersion)
            else
                createFakeUnzippedLibraryWithSpecificVersion(libraryVersion)

            val expectedExitCode = if (expectedWarningStatus == WarningStatus.NO_WARNINGS) ExitCode.OK else ExitCode.COMPILATION_ERROR

            val context = CompilerInvocationContext(
                sourceFile = sourceFile,
                outputDir = outputDir,
                moduleName = moduleName,
                fakeLibraryPath = fakeLibrary.absolute(),
                additionalLibraries = additionalLibraries(),
                exportKlibToOlderAbiVersion = exportKlibToOlderAbiVersion,
                messageCollector = messageCollector,
                expectedExitCode = expectedExitCode,
            )
            runCompiler(context)
        }

        val klibAbiCompatibilityLevel =
            if (exportKlibToOlderAbiVersion) KlibAbiCompatibilityLevel.LATEST_STABLE.previous()!! else KlibAbiCompatibilityLevel.LATEST_STABLE
        messageCollector.checkMessage(expectedWarningStatus, libraryVersion, compilerVersion, klibAbiCompatibilityLevel)
    }

    protected fun createDir(name: String): Path = tmpdir.resolve(name).apply { createDirectories() }
    protected fun createFile(name: String): Path = tmpdir.resolve(name).apply { parent.createDirectories() }

    protected abstract val libraryDisplayName: String
    protected abstract val platformDisplayName: String

    protected fun MessageCollectorImpl.hasOldLibraryError(specificVersions: Pair<TestVersion, TestVersion>? = null): Boolean {
        val stdlibMessagePart = "$platformDisplayName $libraryDisplayName library has an older version" +
                specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    protected fun MessageCollectorImpl.hasTooNewLibraryError(
        libraryVersion: TestVersion? = null,
        abiCompatibilityLevel: KlibAbiCompatibilityLevel? = null,
    ): Boolean {
        val stdlibMessagePart = "The $platformDisplayName $libraryDisplayName library has the ABI version" +
                libraryVersion?.let { " (${it.basicVersion.major}.${it.basicVersion.minor}.0)" }.orEmpty()
        val compilerMessagePart = "that is not compatible with the compiler's current ABI compatibility level ($abiCompatibilityLevel)"

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    protected fun MessageCollectorImpl.checkMessage(
        expectedWarningStatus: WarningStatus,
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        abiCompatibilityLevel: KlibAbiCompatibilityLevel?,
    ) {
        val success = when (expectedWarningStatus) {
            WarningStatus.NO_WARNINGS -> !hasOldLibraryError() && !hasTooNewLibraryError()
            WarningStatus.OLD_LIBRARY_WARNING -> hasOldLibraryError(libraryVersion!! to compilerVersion!!)
            WarningStatus.TOO_NEW_LIBRARY_WARNING -> hasTooNewLibraryError(libraryVersion!!, abiCompatibilityLevel)
        }
        if (!success) fail(
            buildString {
                appendLine("Compiling with stdlib=[$libraryVersion] and compiler=[$compilerVersion]")
                appendLine("Logger compiler messages (${messages.size} items):")
                messages.joinTo(this, "\n")
            }
        )
    }

    @OptIn(ExperimentalPathApi::class)
    protected fun createPatchedLibrary(libraryPath: String): Path {
        val src = Path(libraryPath)
        val stdlibName = if (src.isDirectory()) src.name else src.nameWithoutExtension
        val patchedStdlibDir = createTempDirectory(stdlibName).absolute()
        if (src.isDirectory()) {
            src.copyToRecursively(patchedStdlibDir, followLinks = false, overwrite = true)
        } else {
            src.unzipTo(patchedStdlibDir)
        }
        patchedStdlibDir.resolve(KLIB_JAR_MANIFEST_FILE).deleteIfExists()
        return patchedStdlibDir
    }

    abstract val patchedLibraryPostfix: String
    open fun additionalPatchedLibraryProperties(manifestFile: Path) = Unit

    @OptIn(ExperimentalPathApi::class)
    protected fun createFakeUnzippedLibraryWithSpecificVersion(version: TestVersion?): Path {
        val rawVersion = version?.toString()

        val patchedLibraryDir = createDir("dependencies/fakeLib-${rawVersion ?: "unknown"}-$patchedLibraryPostfix")
        val manifestFile = patchedLibraryDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedLibraryDir

        val originalLibraryFile = originalLibraryPath

        if (originalLibraryFile.isDirectory()) {
            originalLibraryFile.copyToRecursively(patchedLibraryDir, followLinks = false, overwrite = true)
        } else {
            originalLibraryPath.unzipTo(patchedLibraryDir)
            // Zipped version of KLIB always has a manifest file, so we delete it inside the patchedLibraryDir
            // just after unzipping, to replace with the test one
        }

        if (version != null) {
            val properties = manifestFile.readProperties()
            properties[KLIB_PROPERTY_ABI_VERSION] = KotlinAbiVersion(version.basicVersion.major, version.basicVersion.minor, 0).toString()
            manifestFile.writeProperties(properties)
        }

        if (rawVersion != null) {
            val jarManifestFile = patchedLibraryDir.resolve(KLIB_JAR_MANIFEST_FILE)
            jarManifestFile.parent.createDirectories()
            jarManifestFile.outputStream().use { os ->
                with(Manifest()) {
                    mainAttributes.putValue(KLIB_JAR_LIBRARY_VERSION, rawVersion)
                    mainAttributes.putValue("Manifest-Version", "1.0") // some convention stuff to make Jar manifest work
                    write(os)
                }
            }
        }

        additionalPatchedLibraryProperties(manifestFile)

        return patchedLibraryDir
    }

    protected fun createFakeZippedLibraryWithSpecificVersion(version: TestVersion?): Path {
        val rawVersion = version?.toString()

        val patchedLibraryFile = createFile("dependencies/fakeLib-${rawVersion ?: "unknown"}-$patchedLibraryPostfix.klib")
        if (patchedLibraryFile.exists()) return patchedLibraryFile

        val unzippedLibraryDir = createFakeUnzippedLibraryWithSpecificVersion(version)
        unzippedLibraryDir.zipDirAs(patchedLibraryFile)

        return patchedLibraryFile
    }

    protected inline fun <T> withCustomCompilerVersion(version: TestVersion?, block: () -> T): T {
        @Suppress("DEPRECATION")
        return try {
            LibrarySpecialCompatibilityChecker.setUpCustomCompilerVersionForTest(version?.toString())
            block()
        } finally {
            LibrarySpecialCompatibilityChecker.resetUpCustomCompilerVersionForTest()
        }
    }

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
            VERSIONS.map { [patch, postfix] -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor, patch, postfix) }
                .groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        val SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS: List<TestVersion> =
            VERSIONS.map { [patch, postfix] -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor - 1, patch, postfix) }
    }
}
