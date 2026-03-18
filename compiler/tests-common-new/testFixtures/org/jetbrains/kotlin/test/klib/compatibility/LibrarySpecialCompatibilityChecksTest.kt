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
import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import java.io.File
import java.util.*
import java.util.jar.Manifest
import org.jetbrains.kotlin.konan.file.File as KFile

data class CompilerInvocationContext(
    val sourceFile: File,
    val outputDir: File,
    val moduleName: String,
    val fakeLibraryPath: String,
    val additionalLibraries: List<String>,
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
    private lateinit var tmpdir: File

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
        ).forEach { (libraryVersion, compilerVersion) ->
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

    protected abstract val originalLibraryPath: String
    protected open fun additionalLibraries(): List<String> = listOf()

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
                fakeLibraryPath = fakeLibrary.absolutePath,
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

    protected fun createDir(name: String): File = tmpdir.resolve(name).apply { mkdirs() }
    protected fun createFile(name: String): File = tmpdir.resolve(name).apply { parentFile.mkdirs() }

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

    protected fun createPatchedLibrary(libraryPath: String): String {
        val src = File(libraryPath)
        val stdlibName = if (src.isDirectory) src.name else src.nameWithoutExtension
        val patchedStdlibDir = File(createTempDir(stdlibName).absolutePath)
        if (src.isDirectory) {
            src.copyRecursively(patchedStdlibDir, overwrite = true)
        } else {
            KFile(libraryPath).unzipTo(KFile(patchedStdlibDir.absolutePath))
        }
        patchedStdlibDir.resolve(KLIB_JAR_MANIFEST_FILE).delete()
        return patchedStdlibDir.absolutePath
    }

    abstract val patchedLibraryPostfix: String
    open fun additionalPatchedLibraryProperties(manifestFile: File) = Unit

    protected fun createFakeUnzippedLibraryWithSpecificVersion(version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryDir = createDir("dependencies/fakeLib-${rawVersion ?: "unknown"}-$patchedLibraryPostfix")
        val manifestFile = patchedLibraryDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedLibraryDir

        val originalLibraryFile = File(originalLibraryPath)

        if (originalLibraryFile.isDirectory) {
            originalLibraryFile.copyRecursively(patchedLibraryDir)
        } else {
            KFile(originalLibraryPath).unzipTo(KFile(patchedLibraryDir.absolutePath))
            // Zipped version of KLIB always has a manifest file, so we delete it inside the patchedLibraryDir
            // just after unzipping, to replace with the test one
        }

        if (version != null) {
            val properties = manifestFile.inputStream().use { Properties().apply { load(it) } }
            properties[KLIB_PROPERTY_ABI_VERSION] = KotlinAbiVersion(version.basicVersion.major, version.basicVersion.minor, 0).toString()
            manifestFile.outputStream().use { properties.store(it, null) }
        }

        if (rawVersion != null) {
            val jarManifestFile = patchedLibraryDir.resolve(KLIB_JAR_MANIFEST_FILE)
            jarManifestFile.parentFile.mkdirs()
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

    protected fun createFakeZippedLibraryWithSpecificVersion(version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryFile = createFile("dependencies/fakeLib-${rawVersion ?: "unknown"}-$patchedLibraryPostfix.klib")
        if (patchedLibraryFile.exists()) return patchedLibraryFile

        val unzippedLibraryDir = createFakeUnzippedLibraryWithSpecificVersion(version)
        zipDirectory(directory = unzippedLibraryDir, zipFile = patchedLibraryFile)

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
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor, patch, postfix) }
                .groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        val SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS: List<TestVersion> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor - 1, patch, postfix) }
    }
}

private fun zipDirectory(directory: File, zipFile: File) {
    KFile(directory.toPath()).zipDirAs(KFile(zipFile.toPath()))
}
