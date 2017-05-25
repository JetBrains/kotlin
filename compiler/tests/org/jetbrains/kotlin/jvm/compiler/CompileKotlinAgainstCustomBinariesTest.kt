/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.WrongBytecodeVersionTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.config.KotlinCompilerVersion.TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isObject
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream
import kotlin.experimental.xor

class CompileKotlinAgainstCustomBinariesTest : TestCaseWithTmpdir() {
    private val testDataDirectory: File
        get() = File(TEST_DATA_PATH, getTestName(true))

    private fun getTestDataFileWithExtension(extension: String): File {
        return File(testDataDirectory, "${getTestName(true)}.$extension")
    }

    private fun compileLibrary(
            sourcePath: String,
            compiler: CLICompiler<*> = K2JVMCompiler(),
            destination: File = File(tmpdir, "$sourcePath.jar"),
            additionalOptions: List<String> = emptyList(),
            vararg extraClassPath: File
    ): File {
        val output = compileKotlin(sourcePath, destination, extraClassPath.toList(), compiler, additionalOptions, expectedFileName = null)
        assertEquals(normalizeOutput("" to ExitCode.OK), normalizeOutput(output))
        return destination
    }

    private fun normalizeOutput(output: Pair<String, ExitCode>): String {
        return AbstractCliTest.getNormalizedCompilerOutput(output.first, output.second, testDataDirectory.path)
                .replace(FileUtil.toSystemIndependentName(tmpdir.absolutePath), "\$TMP_DIR\$")
    }

    private fun doTestWithTxt(vararg extraClassPath: File) {
        validateAndCompareDescriptorWithFile(
                analyzeFileToPackageView(*extraClassPath),
                AbstractLoadJavaTest.COMPARATOR_CONFIGURATION,
                getTestDataFileWithExtension("txt")
        )
    }

    private fun analyzeFileToPackageView(vararg extraClassPath: File): PackageViewDescriptor {
        val environment = createEnvironment(extraClassPath.toList())

        val ktFile = KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt"))
        val result = JvmResolveUtil.analyzeAndCheckForErrors(ktFile, environment)

        return result.moduleDescriptor.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME).also {
            assertFalse("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, it.isEmpty())
        }
    }

    private fun createEnvironment(extraClassPath: List<File>): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *extraClassPath.toTypedArray())
        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private fun analyzeAndGetAllDescriptors(vararg extraClassPath: File): Collection<DeclarationDescriptor> =
            DescriptorUtils.getAllDescriptors(analyzeFileToPackageView(*extraClassPath).memberScope)

    private fun compileJava(libraryDir: String): File {
        val allJavaFiles = FileUtil.findFilesByMask(JAVA_FILES, File(testDataDirectory, libraryDir))
        val result = File(tmpdir, libraryDir)
        assert(result.mkdirs()) { "Could not create directory: $result" }
        KotlinTestUtils.compileJavaFiles(allJavaFiles, listOf("-d", result.path))
        return result
    }

    private fun compileKotlin(
            fileName: String,
            output: File,
            classpath: List<File> = emptyList(),
            compiler: CLICompiler<*> = K2JVMCompiler(),
            additionalOptions: List<String> = emptyList(),
            expectedFileName: String? = "output.txt"
    ): Pair<String, ExitCode> {
        val args = mutableListOf<String>()
        val sourceFile = File(testDataDirectory, fileName)
        assert(sourceFile.exists()) { "Source file does not exist: ${sourceFile.absolutePath}" }
        args.add(sourceFile.path)

        if (compiler is K2JSCompiler) {
            if (classpath.isNotEmpty()) {
                args.add("-libraries")
                args.add(classpath.joinToString(File.pathSeparator))
            }
            args.add("-output")
            args.add(output.path)
            args.add("-meta-info")
        }
        else if (compiler is K2JVMCompiler) {
            if (classpath.isNotEmpty()) {
                args.add("-classpath")
                args.add(classpath.joinToString(File.pathSeparator))
            }
            args.add("-d")
            args.add(output.path)
        }
        else {
            throw UnsupportedOperationException(compiler.toString())
        }

        args.addAll(additionalOptions)

        val result = AbstractCliTest.executeCompilerGrabOutput(compiler, args)
        if (expectedFileName != null) {
            KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, expectedFileName), normalizeOutput(result))
        }
        return result
    }

    private fun doTestBrokenJavaLibrary(libraryName: String, vararg pathsToDelete: String) {
        // This function compiles a Java library, then deletes one class file and attempts to compile a Kotlin source against
        // this broken library. The expected result is an error message from the compiler
        val library = deletePaths(compileJava(libraryName), *pathsToDelete)
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    private fun doTestBrokenKotlinLibrary(libraryName: String, vararg pathsToDelete: String) {
        // Analogous to doTestBrokenJavaLibrary, but with a Kotlin library compiled to a JAR file
        val library = copyJarFileWithoutEntry(compileLibrary(libraryName), *pathsToDelete)
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersion(
            libraryName: String,
            additionalTransformation: ((fieldName: String, value: Any?) -> Any?)?,
            vararg additionalOptions: String
    ) {
        val version = JvmMetadataVersion(42, 0, 0).toArray()
        val library = transformJar(
                compileLibrary(libraryName),
                { _, bytes ->
                    WrongBytecodeVersionTest.transformMetadataInClassFile(bytes) { fieldName, value ->
                        additionalTransformation?.invoke(fieldName, value) ?:
                        version.takeIf { JvmAnnotationNames.METADATA_VERSION_FIELD_NAME == fieldName }
                    }
                }
        )
        compileKotlin("source.kt", tmpdir, listOf(library), K2JVMCompiler(), additionalOptions.toList())
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersionJs(libraryName: String, vararg additionalOptions: String) {
        compileLibrary(libraryName, K2JSCompiler(), File(tmpdir, "library.js"), emptyList())

        val library = File(tmpdir, "library.meta.js")
        library.writeText(library.readText(Charsets.UTF_8).replace(
                "(" + JsMetadataVersion.INSTANCE.toInteger() + ", ",
                "(" + JsMetadataVersion(42, 0, 0).toInteger() + ", "
        ), Charsets.UTF_8)

        compileKotlin("source.kt", File(tmpdir, "usage.js"), listOf(library), K2JSCompiler(), additionalOptions.toList())
    }

    private fun doTestPreReleaseKotlinLibrary(
            compiler: CLICompiler<*>,
            libraryName: String,
            destination: File,
            result: File,
            usageDestination: File,
            vararg additionalOptions: String
    ) {
        // Compiles the library with the "pre-release" flag, then compiles a usage of this library in the release mode

        try {
            System.setProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, "true")
            compileLibrary(libraryName, compiler, destination)
        }
        finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }

        try {
            System.setProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, "false")
            compileKotlin("source.kt", usageDestination, listOf(result), compiler, additionalOptions.toList())
        }
        finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }
    }

    // ------------------------------------------------------------------------------

    fun testRawTypes() {
        val libraryOutput = compileJava("library")
        compileKotlin("library", libraryOutput, listOf(libraryOutput))
        compileKotlin("main.kt", tmpdir, listOf(libraryOutput))
    }

    fun testDuplicateObjectInBinaryAndSources() {
        val allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"))
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size)
        for (descriptor in allDescriptors) {
            assertTrue("Wrong name: " + descriptor, descriptor.name.asString() == "Lol")
            assertTrue("Should be an object: " + descriptor, isObject(descriptor))
        }
    }

    fun testBrokenJarWithNoClassForObject() {
        val brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class")
        val allDescriptors = analyzeAndGetAllDescriptors(brokenJar)
        assertEmpty("No descriptors should be found: " + allDescriptors, allDescriptors)
    }

    fun testSameLibraryTwiceInClasspath() {
        doTestWithTxt(compileLibrary("library-1"), compileLibrary("library-2"))
    }

    fun testMissingEnumReferencedInAnnotationArgument() {
        doTestWithTxt(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"))
    }

    fun testNoWarningsOnJavaKotlinInheritance() {
        // This test checks that there are no PARAMETER_NAME_CHANGED_ON_OVERRIDE or DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES
        // warnings when subclassing in Kotlin from Java binaries (in case when no parameter names are available for Java classes)

        val libraryOutput = compileJava("library")
        val environment = createEnvironment(listOf(libraryOutput))

        val ktFile = KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt"))
        val result = JvmResolveUtil.analyze(ktFile, environment)
        result.throwIfError()

        AnalyzerWithCompilerReport.reportDiagnostics(
                result.bindingContext.diagnostics,
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        )

        assertEquals("There should be no diagnostics", 0, result.bindingContext.diagnostics.count())
    }

    fun testIncompleteHierarchyInJava() {
        doTestBrokenJavaLibrary("library", "test/Super.class")
    }

    fun testIncompleteHierarchyInKotlin() {
        doTestBrokenKotlinLibrary("library", "test/Super.class")
    }

    fun testMissingDependencySimple() {
        doTestBrokenKotlinLibrary("library", "a/A.class")
    }

    fun testMissingDependencyDifferentCases() {
        doTestBrokenKotlinLibrary("library", "a/A.class")
    }

    fun testMissingDependencyNestedAnnotation() {
        doTestBrokenKotlinLibrary("library", "a/A\$Anno.class")
    }

    fun testMissingDependencyConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(compileLibrary("library1"),
                                               "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class")
        val library2 = copyJarFileWithoutEntry(compileLibrary("library2"),
                                               "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class")
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    fun testMissingDependencyJava() {
        doTestBrokenJavaLibrary("library", "test/Bar.class")
    }

    fun testMissingDependencyJavaConflictingLibraries() {
        val library1 = deletePaths(compileJava("library1"), "test/A.class", "test/A\$Inner.class")
        val library2 = deletePaths(compileJava("library2"), "test/A.class", "test/A\$Inner.class")
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    fun testMissingDependencyJavaNestedAnnotation() {
        doTestBrokenJavaLibrary("library", "test/A\$Anno.class")
    }

    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        val destination = File(tmpdir, "library.jar")
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", destination, destination, tmpdir)
    }

    fun testReleaseCompilerAgainstPreReleaseLibraryJs() {
        doTestPreReleaseKotlinLibrary(
                K2JSCompiler(), "library",
                File(tmpdir, "library.js"), File(tmpdir, "library.meta.js"),
                File(tmpdir, "usage.js")
        )
    }

    fun testReleaseCompilerAgainstPreReleaseLibrarySkipVersionCheck() {
        val destination = File(tmpdir, "library.jar")
        doTestPreReleaseKotlinLibrary(
                K2JVMCompiler(), "library",
                destination, destination, tmpdir,
                "-Xskip-metadata-version-check"
        )
    }

    fun testReleaseCompilerAgainstPreReleaseLibraryJsSkipVersionCheck() {
        doTestPreReleaseKotlinLibrary(
                K2JSCompiler(), "library",
                File(tmpdir, "library.js"), File(tmpdir, "library.meta.js"),
                File(tmpdir, "usage.js"),
                "-Xskip-metadata-version-check"
        )
    }

    fun testWrongMetadataVersion() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null)
    }

    fun testWrongMetadataVersionJs() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library")
    }

    fun testWrongMetadataVersionBadMetadata() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, value ->
            if (JvmAnnotationNames.METADATA_DATA_FIELD_NAME == name) {
                @Suppress("UNCHECKED_CAST")
                val strings = value as Array<String>
                strings.map { string ->
                    String(string.toByteArray().map { x -> x xor 42 }.toTypedArray().toByteArray())
                }.toTypedArray()
            }
            else null
        })
    }

    fun testWrongMetadataVersionBadMetadata2() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, _ ->
            if (JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME == name) arrayOf<String>() else null
        })
    }

    fun testWrongMetadataVersionSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, "-Xskip-metadata-version-check")
    }

    fun testWrongMetadataVersionJsSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library", "-Xskip-metadata-version-check")
    }

    /*test source mapping generation when source info is absent*/
    fun testInlineFunWithoutDebugInfo() {
        compileKotlin("sourceInline.kt", tmpdir)

        val inlineFunClass = File(tmpdir.absolutePath, "test/A.class")
        val cw = ClassWriter(Opcodes.ASM5)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.ASM5, cw) {
            override fun visitSource(source: String?, debug: String?) {
                //skip debug info
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        compileKotlin("source.kt", tmpdir, listOf(tmpdir))

        var debugInfo: String? = null
        val resultFile = File(tmpdir.absolutePath, "test/B.class")
        ClassReader(resultFile.readBytes()).accept(object : ClassVisitor(Opcodes.ASM5) {
            override fun visitSource(source: String?, debug: String?) {
                debugInfo = debug
            }
        }, 0)

        val expected = """
            SMAP
            source.kt
            Kotlin
            *S Kotlin
            *F
            + 1 source.kt
            test/B
            *L
            1#1,13:1
            *E
        """.trimIndent() + "\n"

        if (InlineCodegenUtil.GENERATE_SMAP) {
            assertEquals(expected, debugInfo)
        }
        else {
            assertEquals(null, debugInfo)
        }
    }

    fun testReplaceAnnotationClassWithInterface() {
        val library1 = compileLibrary("library-1")
        val usage = compileLibrary("usage", extraClassPath = *arrayOf(library1))
        val library2 = compileLibrary("library-2")
        doTestWithTxt(usage, library2)
    }

    fun testProhibitNestedClassesByDollarName() {
        val library = compileLibrary("library")
        val javaLibraryOutput = compileJava("library")
        compileKotlin("main.kt", tmpdir, listOf(javaLibraryOutput, library))
    }

    fun testTypeAliasesAreInvisibleInCompatibilityMode() {
        val library = compileLibrary("typeAliases.kt")
        compileKotlin("main.kt", tmpdir, listOf(library), K2JVMCompiler(), listOf("-language-version", "1.0"))
    }

    fun testInnerClassPackageConflict() {
        val output = compileJava("library")
        File(testDataDirectory, "library/test/Foo/x.txt").copyTo(File(output, "test/Foo/x.txt"))
        MockLibraryUtil.createJarFile(tmpdir, output, null, "library", false)
        compileKotlin("source.kt", tmpdir, listOf(File(tmpdir, "library.jar")))
    }

    fun testInnerClassPackageConflict2() {
        val library1 = compileJava("library1")
        val library2 = compileJava("library2")

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

    fun testWrongInlineTarget() {
        val library = compileLibrary("library", additionalOptions = listOf("-jvm-target", "1.8"))
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    companion object {
        private val TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/"
        private val JAVA_FILES = Pattern.compile(".*\\.java$")

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

        private fun deletePaths(library: File, vararg pathsToDelete: String): File {
            for (pathToDelete in pathsToDelete) {
                val fileToDelete = File(library, pathToDelete)
                assert(fileToDelete.delete()) { "Can't delete $fileToDelete" }
            }
            return library
        }
    }
}
