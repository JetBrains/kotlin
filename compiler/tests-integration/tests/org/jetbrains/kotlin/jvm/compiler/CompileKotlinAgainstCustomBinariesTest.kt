/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.cli.transformMetadataInClassFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.forcesPreReleaseBinariesIfEnabled
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.util.toMetadataVersion
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream
import kotlin.experimental.xor

class CompileKotlinAgainstCustomBinariesTest : AbstractKotlinCompilerIntegrationTest() {
    private val languageVersion: LanguageVersion = LanguageVersion.LATEST_STABLE

    override val testDataPath: String
        get() = "compiler/testData/compileKotlinAgainstCustomBinaries/"

    // Compiles Kotlin sources with the language version used in this test, unless the language version is explicitly overridden.
    // If this is the FIR test (so a language version is >= 2.0), use the ".fir.txt" file to check the compilation result if it's present.
    // Note that it also has effect if invoked from `compileLibrary`.
    override fun compileKotlin(
        fileName: String,
        output: File,
        classpath: List<File>,
        compiler: CLICompiler<*>,
        additionalOptions: List<String>,
        expectedFileName: String?,
        additionalSources: List<String>,
        sanitizeCompilerOutput: (String) -> String,
    ): Pair<String, ExitCode> {
        val options =
            if (CommonCompilerArguments::languageVersion.cliArgument in additionalOptions) additionalOptions
            else additionalOptions + listOf(
                CommonCompilerArguments::languageVersion.cliArgument, languageVersion.versionString,
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
            )
        val expectedFirFile = expectedFileName?.replace(".txt", ".fir.txt")?.let { File(testDataDirectory, it) }
        return super.compileKotlin(
            fileName, output, classpath, compiler, options,
            if (expectedFirFile != null && languageVersion.usesK2 && expectedFirFile.exists()) expectedFirFile.name else expectedFileName,
            additionalSources, sanitizeCompilerOutput
        )
    }

    private fun muteForK2(test: () -> Unit) {
        try {
            test()
        } catch (_: Throwable) {
            return
        }
        fail("Looks like this test can be unmuted. Remove the call to `muteForK2`.")
    }

    private fun doTestBrokenLibrary(libraryName: String, vararg pathsToDelete: String, additionalOptions: List<String> = emptyList()) {
        // This function compiles a library, then deletes one class file and attempts to compile a Kotlin source against
        // this broken library. The expected result is an error message from the compiler
        val library = copyJarFileWithoutEntry(compileLibrary(libraryName), *pathsToDelete)
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = additionalOptions)
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersion(
        libraryName: String,
        additionalTransformation: ((fieldName: String, value: Any?) -> Any?)?,
        vararg additionalOptions: String
    ) {
        val library = transformJar(
            compileLibrary(libraryName, additionalOptions = listOf(K2MetadataCompilerArguments::metadataVersion.cliArgument("42.0.0"))),
            { _, bytes ->
                transformMetadataInClassFile(bytes) { fieldName, value ->
                    additionalTransformation?.invoke(fieldName, value)
                }
            }
        )
        compileKotlin("source.kt", tmpdir, listOf(library), K2JVMCompiler(), additionalOptions.toList())
    }

    private fun doTestPreReleaseKotlinLibrary(
        compiler: CLICompiler<*>,
        libraryName: String,
        usageDestination: File,
        vararg additionalOptions: String
    ) {
        // Compiles the library with some non-stable language version, then compiles a usage of this library with stable LV.
        // If there's no non-stable language version yet, the test does nothing.
        val someNonStableVersion =
            LanguageVersion.entries.firstOrNull { it > languageVersion && it > LanguageVersion.LATEST_STABLE } ?: return

        val libraryOptions = listOf(
            CommonCompilerArguments::languageVersion.cliArgument, someNonStableVersion.versionString,
            // Suppress the "language version X is experimental..." warning.
            CommonCompilerArguments::suppressVersionWarnings.cliArgument
        )

        val result =
            when (compiler) {
                is K2JSCompiler -> compileJsLibrary(
                    libraryName,
                    additionalOptions = libraryOptions
                )
                is K2JVMCompiler -> compileLibrary(libraryName, additionalOptions = libraryOptions)
                else -> throw UnsupportedOperationException(compiler.toString())
            }

        compileKotlin("source.kt", usageDestination, listOf(result), compiler, additionalOptions.toList())
    }

    private fun <T> withPreRelease(block: () -> T): T =
        try {
            System.setProperty(KotlinCompilerVersion.TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, "true")
            block()
        } finally {
            System.clearProperty(KotlinCompilerVersion.TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }

    // ------------------------------------------------------------------------------

    // KT-62900 K2: Expected expression to be resolved during Fir2Ir
    @Test
    fun testMissingEnumReferencedInAnnotationArgument() {
        doTestBrokenLibrary("library", "a/E.class")
    }

    @Test
    fun testIncompleteHierarchyInJava() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    @Test
    fun testIncompleteHierarchyInKotlin() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    @Test
    fun testIncompleteHierarchyMissingInterface() {
        doTestBrokenLibrary("library", "test/A.class")
    }

    @Test
    fun testIncompleteHierarchyOnlyImport() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    @Test
    fun testMissingStaticClass() {
        doTestBrokenLibrary("library", $$"test/C$D.class")
    }

    @Test
    fun testIncompleteHierarchyNoErrors() {
        doTestBrokenLibrary(
            "library",
            "test/Super.class",
            additionalOptions = listOf("-XXLanguage:-${LanguageFeature.AllowEagerSupertypeAccessibilityChecks.name}"),
        )
    }

    @Test
    fun testIncompleteHierarchyWithExtendedCompilerChecks() {
        doTestBrokenLibrary(
            "library",
            "test/Super.class",
            additionalOptions = listOf("-XXLanguage:+${LanguageFeature.AllowEagerSupertypeAccessibilityChecks.name}"),
        )
    }

    @Test
    fun testIncompleteHierarchyErrorPositions() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    @Test
    fun testIncompleteHierarchyOfEnclosingClass() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    @Test
    fun testMissingDependencySimple() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    @Test
    fun testComputeSupertypeWithMissingDependency() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    @Test
    fun testMissingDependencyDifferentCases() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    @Test
    fun testMissingDependencyNestedAnnotation() {
        doTestBrokenLibrary("library", $$"a/A$Anno.class")
    }

    @Test
    fun testMissingDependencyConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(
            compileLibrary("library1"),
            "a/A.class", $$"a/A$Inner.class", "a/AA.class", $$"a/AA$Inner.class",
            "a/AAA.class", $$"a/AAA$Inner.class", $$"a/AAA$Inner$Inner.class"
        )
        val library2 = copyJarFileWithoutEntry(
            compileLibrary("library2"),
            "a/A.class", $$"a/A$Inner.class", "a/AA.class", $$"a/AA$Inner.class",
            "a/AAA.class", $$"a/AAA$Inner.class", $$"a/AAA$Inner$Inner.class"
        )
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    @Test
    fun testMissingDependencyJava() {
        doTestBrokenLibrary("library", "test/Bar.class")
    }

    @Test
    fun testMissingDependencyJavaConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(compileLibrary("library1"), "test/A.class", $$"test/A$Inner.class")
        val library2 = copyJarFileWithoutEntry(compileLibrary("library2"), "test/A.class", $$"test/A$Inner.class")
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    @Test
    fun testMissingDependencyJavaNestedAnnotation() {
        doTestBrokenLibrary("library", $$"test/A$Anno.class")
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir)
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibraryJs() {
        doTestPreReleaseKotlinLibrary(
            K2JSCompiler(),
            "library",
            File(
                tmpdir,
                "usage.js"
            )
        )
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipPrereleaseCheck() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir, CommonCompilerArguments::skipPrereleaseCheck.cliArgument)
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck() {
        doTestPreReleaseKotlinLibrary(
            K2JSCompiler(),
            "library",
            File(
                tmpdir,
                "usage.js"
            ),
            CommonCompilerArguments::skipPrereleaseCheck.cliArgument
        )
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipMetadataVersionCheck() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir, K2MetadataCompilerArguments::skipMetadataVersionCheck.cliArgument)
    }

    @Test
    fun testPreReleaseCompilerAgainstPreReleaseLibraryStableLanguageVersion() {
        withPreRelease {
            val library = compileLibrary("library")
            val someStableReleasedVersion = LanguageVersion.entries.first { it.isStable && it >= LanguageVersion.FIRST_NON_DEPRECATED }
            compileKotlin(
                "source.kt", tmpdir, listOf(library), K2JVMCompiler(),
                listOf(CommonCompilerArguments::languageVersion.cliArgument, someStableReleasedVersion.versionString)
            )

            checkPreReleaseness(File(tmpdir, "usage/SourceKt.class"), shouldBePreRelease = false)
        }
    }

    @Test
    fun testPreReleaseCompilerAgainstPreReleaseLibraryLatestStable() {
        withPreRelease {
            val library = compileLibrary("library")
            compileKotlin(
                "source.kt", tmpdir, listOf(library), K2JVMCompiler(),
                listOf(CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.LATEST_STABLE.versionString)
            )

            checkPreReleaseness(File(tmpdir, "usage/SourceKt.class"), shouldBePreRelease = true)
        }
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipPrereleaseCheckAllowUnstableDependencies() {
        doTestPreReleaseKotlinLibrary(
            K2JVMCompiler(),
            "library",
            tmpdir,
            K2JVMCompilerArguments::allowUnstableDependencies.cliArgument,
            CommonCompilerArguments::skipPrereleaseCheck.cliArgument
        )
    }

    @Test
    fun testWrongMetadataVersion() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null)
    }

    // This test compiles a library with a "future" metadata version, then intentionally inserts some gibberish to the metadata and tries
    // to compile something against this library. It emulates the scenario when a future Kotlin version has a completely different metadata
    // format -- so different that reading it as if it's the current (protobuf-based) format would most likely result in an exception.
    // The expected result is that the compiler does NOT try to read it, and instead reports incompatible version & unresolved reference errors.
    @Test
    fun testWrongMetadataVersionBadMetadata() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, value ->
            if (JvmAnnotationNames.METADATA_DATA_FIELD_NAME == name) {
                @Suppress("UNCHECKED_CAST")
                val strings = value as Array<String>
                strings.map { string ->
                    String(string.toByteArray().map { x -> x xor 42 }.toTypedArray().toByteArray())
                }.toTypedArray()
            } else null
        })
    }

    @Test
    fun testWrongMetadataVersionBadMetadata2() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, _ ->
            if (JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME == name) arrayOf<String>() else null
        })
    }

    @Test
    fun testWrongMetadataVersionSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, K2MetadataCompilerArguments::skipMetadataVersionCheck.cliArgument)
    }

    @Test
    fun testWrongMetadataVersionSkipPrereleaseCheckHasNoEffect() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, CommonCompilerArguments::skipPrereleaseCheck.cliArgument)
    }

    @Test
    fun testKlibMetadataVersionSupportedDependency() {
        val sourceSetDir = File(testDataPath, "klibMetadataVersionDependency")
        val currentLanguageVersionIndex = LanguageVersion.entries.indexOf(LanguageVersion.LATEST_STABLE)
        val supportedKlibDependencyMetadataVersions = listOf(
            LanguageVersion.entries[currentLanguageVersionIndex - 2],
            LanguageVersion.entries[currentLanguageVersionIndex - 1],
            LanguageVersion.LATEST_STABLE,
            LanguageVersion.entries[currentLanguageVersionIndex + 1],
        ).map { it.toMetadataVersion().toString() }

        val library = File(tmpdir, "lib1")
        val usage = File(tmpdir, "lib2")
        val commonStdlib = StandardLibrariesPathProviderForKotlinProject.commonStdlibForTests()
        val metadataKlibOptions = listOf(
            K2MetadataCompilerArguments::metadataKlib.cliArgument,
            "-Xtarget-platform=JVM,JS,WasmJs,WasmWasi,Native",
            CommonCompilerArguments::languageVersion.cliArgument,
            languageVersion.versionString,
            CommonCompilerArguments::suppressVersionWarnings.cliArgument,
        )
        for (metadataVersion in supportedKlibDependencyMetadataVersions) {
            val libraryResult = AbstractCliTest.executeCompilerGrabOutput(
                KotlinMetadataCompiler(),
                listOf(
                    sourceSetDir.resolve("lib1.kt").path,
                    K2JVMCompilerArguments::classpath.cliArgument,
                    commonStdlib.path,
                    K2JVMCompilerArguments::destination.cliArgument,
                    library.path,
                    CommonCompilerArguments::metadataVersion.cliArgument(metadataVersion),
                    CommonCompilerArguments::skipMetadataVersionCheck.cliArgument,
                ) + metadataKlibOptions
            )
            assertEquals(ExitCode.OK, libraryResult.second)

            val result = AbstractCliTest.executeCompilerGrabOutput(
                KotlinMetadataCompiler(),
                listOf(
                    sourceSetDir.resolve("lib2.kt").path,
                    K2JVMCompilerArguments::classpath.cliArgument,
                    listOf(library, commonStdlib).joinToString(File.pathSeparator),
                    K2JVMCompilerArguments::destination.cliArgument,
                    usage.path,
                ) + metadataKlibOptions
            )
            assertEquals(ExitCode.OK, result.second)
        }
    }

    @Test
    fun testKlibMetadataVersionUnsupportedDependency() {
        val sourceSetDir = File(testDataPath, "klibMetadataVersionDependency")
        val currentLanguageVersionIndex = LanguageVersion.entries.indexOf(LanguageVersion.LATEST_STABLE)
        val unsupportedKlibDependencyMetadataVersion =
            LanguageVersion.entries[currentLanguageVersionIndex + 2].toMetadataVersion().toString()

        val library = File(tmpdir, "lib1")
        val usage = File(tmpdir, "lib2")
        val commonStdlib = StandardLibrariesPathProviderForKotlinProject.commonStdlibForTests()
        val metadataKlibOptions = listOf(
            K2MetadataCompilerArguments::metadataKlib.cliArgument,
            "-Xtarget-platform=JVM,JS,WasmJs,WasmWasi,Native",
            CommonCompilerArguments::languageVersion.cliArgument,
            languageVersion.versionString,
            CommonCompilerArguments::suppressVersionWarnings.cliArgument,
        )
        val libraryResult = AbstractCliTest.executeCompilerGrabOutput(
            KotlinMetadataCompiler(),
            listOf(
                sourceSetDir.resolve("lib1.kt").path,
                K2JVMCompilerArguments::classpath.cliArgument,
                commonStdlib.path,
                K2JVMCompilerArguments::destination.cliArgument,
                library.path,
                CommonCompilerArguments::metadataVersion.cliArgument(unsupportedKlibDependencyMetadataVersion),
                CommonCompilerArguments::skipMetadataVersionCheck.cliArgument,
            ) + metadataKlibOptions
        )
        assertEquals(ExitCode.OK, libraryResult.second)

        val result = AbstractCliTest.executeCompilerGrabOutput(
            KotlinMetadataCompiler(),
            listOf(
                sourceSetDir.resolve("lib2.kt").path,
                K2JVMCompilerArguments::classpath.cliArgument,
                listOf(library, commonStdlib).joinToString(File.pathSeparator),
                K2JVMCompilerArguments::destination.cliArgument,
                usage.path,
            ) + metadataKlibOptions
        )
        val output = result.first

        assertEquals(ExitCode.COMPILATION_ERROR, result.second)
        assertTrue(output.contains("compiled with an incompatible version of Kotlin"))
        assertTrue(output.contains("The actual metadata version is $unsupportedKlibDependencyMetadataVersion"))
    }

    // KT-59901 K2: Disappeared API_NOT_AVAILABLE
    @Test
    fun testRequireKotlin() = muteForK2 {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    // KT-59901 K2: Disappeared API_NOT_AVAILABLE
    @Test
    fun testRequireKotlinInNestedClasses() = muteForK2 {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    // KT-59901 K2: Disappeared API_NOT_AVAILABLE
    @Test
    fun testRequireKotlinInNestedClassesJs() = muteForK2 {
        compileKotlin(
            "source.kt",
            File(
                tmpdir,
                "usage.js"
            ),
            listOf(
                compileJsLibrary(
                    "library"
                )
            ),
            K2JSCompiler()
        )
    }

    // KT-59901 K2: Disappeared API_NOT_AVAILABLE
    @Test
    fun testRequireKotlinInNestedClassesAgainst14Js() = muteForK2 {
        val library1 =
            compileJsLibrary(
                "library",
                additionalOptions = listOf(
                    CommonCompilerArguments::metadataVersion.cliArgument("1.4.0")
                )
            )
        compileKotlin(
            "source.kt",
            File(tmpdir, "usage.js"),
            listOf(library1),
            K2JSCompiler(),
            additionalOptions = listOf(K2MetadataCompilerArguments::skipMetadataVersionCheck.cliArgument)
        )
    }

    @Test
    fun testStrictMetadataVersionSemanticsSameVersion() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(K2JVMCompilerArguments::strictMetadataVersionSemantics.cliArgument)
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    @Test
    fun testMetadataVersionDerivedFromLanguage() {
        for (languageVersion in LanguageVersion.entries) {
            if (languageVersion.isUnsupported) continue

            compileKotlin(
                "source.kt",
                tmpdir,
                additionalOptions = listOf(CommonCompilerArguments::languageVersion.cliArgument, languageVersion.versionString),
                expectedFileName = null
            )

            // Starting from Kotlin 1.4, the major.minor version of JVM metadata must be equal to the language version.
            // From Kotlin 1.0 to 1.4, we used JVM metadata version 1.1.*.
            val expectedMajor = 2
            val expectedMinor = languageVersion.minor

            val topLevelClass = LocalFileKotlinClass.create(File(tmpdir.absolutePath, "Foo.class"), languageVersion.toMetadataVersion())!!
            val classVersion = topLevelClass.classHeader.metadataVersion
            assertEquals(expectedMajor, classVersion.major, "Actual version: $classVersion")
            assertEquals(expectedMinor, classVersion.minor, "Actual version: $classVersion")

            val moduleFile = File(tmpdir.absolutePath, "META-INF/main.kotlin_module").readBytes()
            val versionNumber = ModuleMapping.readVersionNumber(DataInputStream(ByteArrayInputStream(moduleFile)))!!
            val moduleVersion = MetadataVersion(*versionNumber)
            if (languageVersion == LanguageVersion.KOTLIN_2_0) {
                assertEquals(MetadataVersion(1, 9, 9999), moduleVersion, "Actual version: $moduleVersion")
            } else {
                assertEquals(expectedMajor, moduleVersion.major, "Actual version: $moduleVersion")
                assertEquals(expectedMinor, moduleVersion.minor, "Actual version: $moduleVersion")
            }
        }
    }

    /*test source mapping generation when source info is absent*/
    @Test
    fun testInlineFunWithoutDebugInfo() {
        compileKotlin("sourceInline.kt", tmpdir)

        val inlineFunClass = File(tmpdir.absolutePath, "test/A.class")
        val cw = ClassWriter(Opcodes.API_VERSION)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION, cw) {
            override fun visitSource(source: String?, debug: String?) {
                //skip debug info
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        compileKotlin("source.kt", tmpdir, listOf(tmpdir))

        val resultFile = File(tmpdir.absolutePath, "test/B.class")
        ClassReader(resultFile.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitSource(source: String?, debug: String?) {
                assertEquals(null, debug)
            }
        }, 0)
    }

    /* Regression test for KT-37107: compile against .class file without any constructors. */
    @Test
    fun testClassfileWithoutConstructors() {
        compileKotlin("TopLevel.kt", tmpdir, expectedFileName = "TopLevel.txt")

        val inlineFunClass = File(tmpdir.absolutePath, "test/TopLevelKt.class")
        val cw = ClassWriter(Opcodes.API_VERSION)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION, cw) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                if (desc == JvmAnnotationNames.METADATA_DESC) null else super.visitAnnotation(desc, visible)

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                assertEquals("foo", name) // test sanity: shouldn't see any constructors, only the "foo" method
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        val [_, exitCode] = compileKotlin("shouldNotCompile.kt", tmpdir, listOf(tmpdir))
        assertEquals(1, exitCode.code) // double-check that we failed :) output.txt also says so
    }

    @Test
    fun testReplaceAnnotationClassWithInterface() {
        val library1 = compileLibrary("library-1")
        val usage = compileLibrary("usage", extraClassPath = listOf(library1))
        val library2 = compileLibrary("library-2")
        compileKotlin("source.kt", tmpdir, listOf(usage, library2))
    }

    @Test
    fun testInnerClassPackageConflict() {
        val output = compileLibrary("library", destination = File(tmpdir, "library"))
        File(testDataDirectory, "library/test/Foo/x.txt").copyTo(File(output, "test/Foo/x.txt"))
        MockLibraryUtil.createJarFile(tmpdir, output, "library")
        compileKotlin("source.kt", tmpdir, listOf(File(tmpdir, "library.jar")))
    }

    @Test
    fun testInnerClassPackageConflict2() {
        val library1 = compileLibrary("library1", destination = File(tmpdir, "library1"))
        val library2 = compileLibrary("library2", destination = File(tmpdir, "library2"))

        // Copy everything from library2 to library1
        FileUtil.visitFiles(library2) { file ->
            if (!file.isDirectory) {
                val newFile = File(library1, file.relativeTo(library2).path)
                if (!newFile.parentFile.exists()) {
                    assert(newFile.parentFile.mkdirs())
                }
                assert(file.renameTo(newFile))
            }
            true
        }

        compileKotlin("source.kt", tmpdir, listOf(library1))
    }

    @Test
    fun testInlineFunctionsWithMatchingJvmSignatures() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf("-XXLanguage:+InlineClasses"),
            checkKotlinOutput = { _ -> }
        )
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-XXLanguage:+InlineClasses"))

        URLClassLoader(arrayOf(library.toURI().toURL(), tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeJarClassLoader())
            .loadClass("SourceKt").getDeclaredMethod("run").invoke(null)
    }

    @Test
    fun testChangedEnumsInLibrary() {
        val oldLibrary = compileLibrary("old", checkKotlinOutput = {})
        val newLibrary = compileLibrary("new", checkKotlinOutput = {})
        compileKotlin("source.kt", tmpdir, listOf(oldLibrary))

        val result =
            URLClassLoader(arrayOf(newLibrary.toURI().toURL(), tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeJarClassLoader())
                .loadClass("SourceKt").getDeclaredMethod("run").invoke(null) as String
        assertEquals("ABCAB", result)
    }

    @Test
    fun testContextualDeclarationUse() {
        val library = compileLibrary("library")
        compileKotlin(
            "contextualDeclarationUse.kt", tmpdir, listOf(library),
            additionalOptions = listOf(
                CommonCompilerArguments::skipPrereleaseCheck.cliArgument,
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.entries.lastOrNull { it < LanguageFeature.ContextParameters.sinceVersion!! }?.versionString
                    ?: error("No language version found where context parameters aren't enabled by default. Consider dropping this test.")
            )
        )
    }

    // KT-60531 K2/JS: Report diagnostics before running FIR2IR
    @Test
    fun testInternalFromForeignModuleJs() = muteForK2 {
        compileKotlin(
            "source.kt",
            File(tmpdir, "usage.js"),
            listOf(compileJsLibrary("library")),
            K2JSCompiler(),
        )
    }

    @Test
    fun testInternalFromFriendModuleJs() {
        val library1 = compileJsLibrary("library")
        compileKotlin(
            "source.kt",
            File(tmpdir, "usage.js"),
            listOf(library1),
            K2JSCompiler(),
            listOf(K2JSCompilerArguments::friendModules.cliArgument(library1.path))
        )
    }

    /*
    // TODO: see KT-15661 and KT-23483
    @Test
    fun testInternalFromForeignModuleCommon() {
        compileKotlin("source.kt", tmpdir, listOf(compileCommonLibrary("library")), K2MetadataCompiler())
    }
    */

    @Test
    fun testInternalFromFriendModuleCommon() {
        val library = compileCommonLibrary("library")
        compileKotlin(
            fileName = "source.kt",
            output = tmpdir,
            classpath = listOf(library, StandardLibrariesPathProviderForKotlinProject.commonStdlibForTests()),
            compiler = KotlinMetadataCompiler(),
            additionalOptions = listOf(
                K2MetadataCompilerArguments::friendPaths.cliArgument(library.path),
                "-Xtarget-platform=JVM,JS,WasmJs,WasmWasi,Native",
            )
        )
    }

    @Test
    fun testInlineAnonymousObjectWithDifferentTarget() {
        val library = compileLibrary("library", additionalOptions = listOf("-jvm-target", JvmTarget.JVM_1_8.description))
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-jvm-target", JvmTarget.JVM_9.description))
        for (name in listOf("SourceKt", $$$"SourceKt$main$$inlined$foo$1")) {
            val node = ClassNode()
            ClassReader(File(tmpdir, "$name.class").readBytes()).accept(node, 0)
            assertEquals(JvmTarget.JVM_9.majorVersion, node.version)
        }
    }

    @Test
    fun testIncorrectJavaSignature() {
        compileKotlin(
            "source.kt", tmpdir,
            listOf(),
            additionalSources = listOf("A.java", "B.java"),
        )
    }

    @Test
    fun testIncorrectRemoveSignature() {
        compileKotlin(
            "source.kt", tmpdir,
            listOf(),
            additionalSources = listOf("A.java", "B.java"),
        )
    }

    @Test
    fun testAgainstStable() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument, "2.1",
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
            )
        )
        compileKotlin("source.kt", tmpdir, listOf(library))

        val library2 = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument, "2.1",
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                K2JVMCompilerArguments::abiStability.cliArgument("stable")
            )
        )
        compileKotlin("source.kt", tmpdir, listOf(library2))
    }

    @Test
    fun testAgainstFir() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.LATEST_STABLE.versionString)
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    @Test
    fun testAgainstFirWithUnstableAbi() {
        val library2 = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.LATEST_STABLE.versionString,
                K2JVMCompilerArguments::abiStability.cliArgument("unstable")
            )
        )
        compileKotlin("source.kt", tmpdir, listOf(library2))
    }

    @Test
    fun testAgainstUnstable() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument, "2.1",
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                K2JVMCompilerArguments::abiStability.cliArgument("unstable")
            )
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    @Test
    fun testAgainstFirWithStableAbi() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.LATEST_STABLE.versionString,
                K2JVMCompilerArguments::abiStability.cliArgument("stable")
            )
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    @Test
    fun testAgainstFirWithStableAbiAndNoPrereleaseCheck() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.LATEST_STABLE.versionString,
                K2JVMCompilerArguments::abiStability.cliArgument("stable")
            )
        )
        compileKotlin(
            "source.kt", tmpdir, listOf(library), additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument, "2.1",
                CommonCompilerArguments::suppressVersionWarnings.cliArgument,
                CommonCompilerArguments::skipPrereleaseCheck.cliArgument,
            )
        )
    }

    @Test
    fun testAgainstFirWithAllowUnstableDependencies() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.LATEST_STABLE.versionString)
        )
        compileKotlin(
            "source.kt", tmpdir, listOf(library),
            additionalOptions = listOf(
                K2JVMCompilerArguments::allowUnstableDependencies.cliArgument,
                K2JVMCompilerArguments::skipMetadataVersionCheck.cliArgument
            )
        )
    }

    @Test
    fun testUnreachableExtensionVarPropertyDeclaration() {
        val [output, exitCode] = compileKotlin("source.kt", tmpdir, expectedFileName = null)
        assertEquals(ExitCode.COMPILATION_ERROR, exitCode, "Output:\n$output")
    }

    @Test
    fun testUnreachableExtensionValPropertyDeclaration() {
        val [output, exitCode] = compileKotlin("source.kt", tmpdir, expectedFileName = null)
        assertEquals(ExitCode.COMPILATION_ERROR, exitCode, "Output:\n$output")
    }

    @Test
    fun testAnonymousObjectTypeMetadata() {
        doTestAnonymousObjectTypeMetadata()
    }

    @Test
    fun testAnonymousObjectTypeMetadataKlib() {
        doTestAnonymousObjectTypeMetadata(listOf(K2MetadataCompilerArguments::metadataKlib.cliArgument))
    }

    /**
     * This test does exactly the same as [testAnonymousObjectTypeMetadataKlib] but using the old (now deprecated)
     * CLI argument `-Xexpect-actual-linker` instead of its successor `-Xmetadata-klib`.
     *
     * The test is needed only to check that the old CLI argument still works as needed.
     */
    @Test
    fun testAnonymousObjectTypeMetadataKlibWithOldCLIKey() {
        doTestAnonymousObjectTypeMetadata(listOf("-Xexpect-actual-linker")) { output ->
            output.lines().filterNot { "argument -Xexpect-actual-linker is deprecated" in it }.joinToString("\n")
        }
    }

    @Test
    fun testUsageOfNestedTypeAliasesWhenTheyAreNotStable() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.LATEST_STABLE.versionString)
        )
        compileKotlin(
            "source.kt", tmpdir, listOf(library),
            additionalOptions = listOf(CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.KOTLIN_2_2.versionString)
        )
    }

    @Test
    fun testUsageOfNestedTypeAliasesWhenTheyAreEnabled() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.KOTLIN_2_2.versionString,
                CommonCompilerArguments::nestedTypeAliases.cliArgument,
            )
        )
        compileKotlin(
            "source.kt", tmpdir, listOf(library),
            additionalOptions = listOf(
                CommonCompilerArguments::languageVersion.cliArgument,
                LanguageVersion.KOTLIN_2_2.versionString,
                CommonCompilerArguments::nestedTypeAliases.cliArgument,
            )
        )
    }

    @Test
    fun testHasStableParameterNames() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    @Test
    fun testStrictMetadataVersionSemanticsOldVersion() {
        val nextMetadataVersion = languageVersion.toMetadataVersion().next()
        val library = compileLibrary(
            "library", additionalOptions = listOf("-Xgenerate-strict-metadata-version", "-Xmetadata-version=$nextMetadataVersion")
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    @Test
    fun testPreReleaseFlagIsConsistentBetweenBootstrapAndCurrentCompiler() {
        val bootstrapCompiler = JarFile(PathUtil.kotlinPathsForCompiler.compilerPath)
        val classFromBootstrapCompiler = bootstrapCompiler.getEntry(LanguageFeature::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            bootstrapCompiler.getInputStream(classFromBootstrapCompiler).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }

    @Test
    fun testPreReleaseFlagIsConsistentBetweenStdlibAndCurrentCompiler() {
        val stdlib = JarFile(PathUtil.kotlinPathsForCompiler.stdlibPath)
        val classFromStdlib = stdlib.getEntry(KotlinVersion::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            stdlib.getInputStream(classFromStdlib).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseFeatureJs() {
        val arbitraryPoisoningFeature = LanguageFeature.entries.firstOrNull {
            it.forcesPreReleaseBinariesIfEnabled(LanguageVersion.LATEST_STABLE)
        } ?: return

        val poisonedLibrary = compileJsLibrary(
            libraryName = "poisonedLibrary",
            additionalOptions = listOf("-XXLanguage:+$arbitraryPoisoningFeature")
        ) {}

        val library = compileJsLibrary(
            libraryName = "library"
        ) {}

        compileKotlin(
            fileName = "source.kt",
            output = File(tmpdir, "usage.js"),
            classpath = listOf(poisonedLibrary, library),
            compiler = K2JSCompiler()
        ) { compilerOutput ->
            compilerOutput.replace(arbitraryPoisoningFeature.name, "<!POISONING_LANGUAGE_FEATURE!>")
        }
    }

    @Test
    fun testReleaseCompilerWithoutUsageOfPreReleaseFeatureJs() {
        val arbitraryPoisoningFeature = LanguageFeature.entries.firstOrNull {
            it.forcesPreReleaseBinariesIfEnabled(LanguageVersion.LATEST_STABLE)
        } ?: return

        val poisonedLibrary = compileJsLibrary(
            libraryName = "poisonedLibrary",
            additionalOptions = listOf("-XXLanguage:+$arbitraryPoisoningFeature")
        ) {}

        val library = compileJsLibrary(
            libraryName = "library"
        ) {}

        compileKotlin(
            fileName = "source.kt",
            output = File(tmpdir, "usage.js"),
            classpath = listOf(poisonedLibrary, library),
            compiler = K2JSCompiler()
        ) { compilerOutput ->
            compilerOutput.replace(arbitraryPoisoningFeature.name, "<!POISONING_LANGUAGE_FEATURE!>")
        }
    }

    @Test
    fun testDataClassCompiledWith1_0_5Compiler() {
        val library = File(testDataDirectory, "VeryOldLibraryWithDataClass.jar")
        compileKotlin("source.kt", tmpdir, listOf(library), K2JVMCompiler())
    }

    @Test
    fun testAgainstHeaderMode() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xheader-mode"))

        compileKotlin(fileName = "main.kt", output = tmpdir, classpath = listOf(library))
    }

    private fun doTestAnonymousObjectTypeMetadata(
        extraCommandLineArguments: List<String> = emptyList(),
        filterOutput: (String) -> String = { output -> output }
    ) {
        val library = compileCommonLibrary(
            libraryName = "library",
            additionalOptions = extraCommandLineArguments,
            checkKotlinOutput = { output ->
                assertEquals(normalizeOutput("" to ExitCode.OK), filterOutput(output))
            }
        )

        compileKotlin(
            fileName = "anonymousObjectTypeMetadata.kt",
            output = tmpdir,
            classpath = listOf(library, StandardLibrariesPathProviderForKotlinProject.commonStdlibForTests()),
            compiler = KotlinMetadataCompiler(),
            additionalOptions = extraCommandLineArguments + "-Xtarget-platform=JVM,JS,WasmJs,WasmWasi,Native",
        )
    }

    companion object {
        @JvmStatic
        private fun copyJarFileWithoutEntry(jarPath: File, vararg entriesToDelete: String): File =
            transformJar(jarPath, { _, bytes -> bytes }, entriesToDelete.toSet())

        private fun transformJar(
            jarPath: File,
            transformEntry: (String, ByteArray) -> ByteArray,
            entriesToDelete: Set<String> = emptySet()
        ): File {
            val outputFile = File(jarPath.parentFile, "${jarPath.nameWithoutExtension}-after.jar")

            JarFile(jarPath).use { jar ->
                ZipOutputStream(outputFile.outputStream().buffered()).use { output ->
                    for (jarEntry in jar.entries()) {
                        val name = jarEntry.name
                        if (name in entriesToDelete) continue

                        val bytes = jar.getInputStream(jarEntry).readBytes()
                        val newBytes = if (name.endsWith(".class")) transformEntry(name, bytes) else bytes
                        val newEntry = JarEntry(name)
                        newEntry.size = newBytes.size.toLong()
                        output.putNextEntry(newEntry)
                        output.write(newBytes)
                        output.closeEntry()
                    }
                }
            }

            return outputFile
        }
    }

    private fun checkPreReleaseness(classFileBytes: ByteArray, shouldBePreRelease: Boolean) {
        // If there's no "xi" field in the Metadata annotation, it's value is assumed to be 0, i.e. _not_ pre-release
        var isPreRelease = false

        ClassReader(classFileBytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (desc != JvmAnnotationNames.METADATA_DESC) return null

                return object : AnnotationVisitor(Opcodes.API_VERSION) {
                    override fun visit(name: String, value: Any) {
                        if (name != JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME) return

                        isPreRelease = (value as Int and JvmAnnotationNames.METADATA_PRE_RELEASE_FLAG) != 0
                    }
                }
            }
        }, 0)

        assertEquals(shouldBePreRelease, isPreRelease, "Pre-release flag of the class file has incorrect value")
    }

    private fun checkPreReleaseness(file: File, shouldBePreRelease: Boolean) {
        checkPreReleaseness(file.readBytes(), shouldBePreRelease)
    }
}
