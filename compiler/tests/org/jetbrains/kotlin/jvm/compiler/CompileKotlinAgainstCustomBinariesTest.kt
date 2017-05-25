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

import com.google.common.collect.Iterables
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ArrayUtil
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
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.join
import org.jetbrains.kotlin.utils.rethrow
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream
import kotlin.experimental.xor

class CompileKotlinAgainstCustomBinariesTest : TestCaseWithTmpdir() {
    private val testDataDirectory: File
        get() = File(TEST_DATA_PATH, getTestName(true))

    private fun getTestDataFileWithExtension(extension: String): File {
        return File(testDataDirectory, getTestName(true) + "." + extension)
    }

    private fun compileLibrary(sourcePath: String, vararg extraClassPath: File): File {
        return compileLibrary(sourcePath, emptyList<String>(), *extraClassPath)
    }

    private fun compileLibrary(sourcePath: String, additionalOptions: List<String>, vararg extraClassPath: File): File {
        val destination = File(tmpdir, sourcePath + ".jar")
        compileLibrary(K2JVMCompiler(), sourcePath, destination, additionalOptions, *extraClassPath)
        return destination
    }

    private fun compileLibrary(
            compiler: CLICompiler<*>, sourcePath: String, destination: File, additionalOptions: List<String>, vararg extraClassPath: File
    ) {
        val output = compileKotlin(compiler, sourcePath, destination, additionalOptions, *extraClassPath)
        Assert.assertEquals(normalizeOutput(Pair("", ExitCode.OK)), normalizeOutput(output))
    }

    private fun normalizeOutput(output: Pair<String, ExitCode>): String {
        return AbstractCliTest.getNormalizedCompilerOutput(output.first, output.second, testDataDirectory.path)
                .replace(FileUtil.toSystemIndependentName(tmpdir.absolutePath), "\$TMP_DIR$")
    }

    @Throws(Exception::class)
    private fun doTestWithTxt(vararg extraClassPath: File) {
        val packageView = analyzeFileToPackageView(*extraClassPath)

        val comparator = AbstractLoadJavaTest.COMPARATOR_CONFIGURATION
                .withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
        validateAndCompareDescriptorWithFile(packageView, comparator, getTestDataFileWithExtension("txt"))
    }

    @Throws(IOException::class)
    private fun analyzeFileToPackageView(vararg extraClassPath: File): PackageViewDescriptor {
        val environment = createEnvironment(Arrays.asList(*extraClassPath))

        val result = JvmResolveUtil.analyzeAndCheckForErrors(
                KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt")), environment
        )

        val packageView = result.moduleDescriptor.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME)
        assertFalse("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, packageView.isEmpty())
        return packageView
    }

    private fun createEnvironment(extraClassPath: List<File>): KotlinCoreEnvironment {
        val extras = ArrayList<File>()
        extras.addAll(extraClassPath)
        extras.add(KotlinTestUtils.getAnnotationsJar())

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *extras.toTypedArray())
        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    @Throws(IOException::class)
    private fun analyzeAndGetAllDescriptors(vararg extraClassPath: File): Collection<DeclarationDescriptor> {
        return DescriptorUtils.getAllDescriptors(analyzeFileToPackageView(*extraClassPath).memberScope)
    }

    @Throws(Exception::class)
    private fun compileJava(libraryDir: String): File {
        val allJavaFiles = FileUtil.findFilesByMask(JAVA_FILES, File(testDataDirectory, libraryDir))
        val result = File(tmpdir, libraryDir)
        assert(result.mkdirs()) { "Could not create directory: " + result }
        KotlinTestUtils.compileJavaFiles(allJavaFiles, Arrays.asList("-d", result.path))
        return result
    }

    private fun compileKotlin(
            fileName: String,
            output: File,
            vararg classpath: File
    ): Pair<String, ExitCode> {
        return compileKotlin(fileName, output, emptyList<String>(), *classpath)
    }

    private fun compileKotlin(
            fileName: String,
            output: File,
            additionalOptions: List<String>,
            vararg classpath: File
    ): Pair<String, ExitCode> {
        return compileKotlin(K2JVMCompiler(), fileName, output, additionalOptions, *classpath)
    }

    private fun compileKotlin(
            compiler: CLICompiler<*>,
            fileName: String,
            output: File,
            additionalOptions: List<String>,
            vararg classpath: File
    ): Pair<String, ExitCode> {
        val args = ArrayList<String>()
        val sourceFile = File(testDataDirectory, fileName)
        assert(sourceFile.exists()) { "Source file does not exist: " + sourceFile.absolutePath }
        args.add(sourceFile.path)

        if (compiler is K2JSCompiler) {
            if (classpath.isNotEmpty()) {
                args.add("-libraries")
                args.add(join(Arrays.asList(*classpath), File.pathSeparator))
            }
            args.add("-output")
            args.add(output.path)
            args.add("-meta-info")
        }
        else if (compiler is K2JVMCompiler) {
            if (classpath.isNotEmpty()) {
                args.add("-classpath")
                args.add(join(Arrays.asList(*classpath), File.pathSeparator))
            }
            args.add("-d")
            args.add(output.path)
        }
        else {
            throw UnsupportedOperationException(compiler.toString())
        }

        args.addAll(additionalOptions)

        return AbstractCliTest.executeCompilerGrabOutput(compiler, args)
    }

    @Throws(Exception::class)
    private fun doTestBrokenJavaLibrary(libraryName: String, vararg pathsToDelete: String) {
        // This function compiles a Java library, then deletes one class file and attempts to compile a Kotlin source against
        // this broken library. The expected result is an error message from the compiler
        val library = deletePaths(compileJava(libraryName), *pathsToDelete)

        val output = compileKotlin("source.kt", tmpdir, library)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
    private fun doTestBrokenKotlinLibrary(libraryName: String, vararg pathsToDelete: String) {
        // Analogous to doTestBrokenJavaLibrary, but with a Kotlin library compiled to a JAR file
        val library = copyJarFileWithoutEntry(compileLibrary(libraryName), *pathsToDelete)
        val output = compileKotlin("source.kt", tmpdir, library)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
    private fun doTestKotlinLibraryWithWrongMetadataVersion(
            libraryName: String,
            additionalTransformation: ((fieldName: String, value: Any?) -> Any?)?,
            vararg additionalOptions: String
    ) {
        val version = JvmMetadataVersion(42, 0, 0).toArray()
        val library = transformJar(
                compileLibrary(libraryName),
                { entryName, bytes ->
                    WrongBytecodeVersionTest.transformMetadataInClassFile(bytes) { fieldName, value ->
                        if (additionalTransformation != null) {
                            val result = additionalTransformation.invoke(fieldName, value)
                            if (result != null) return@transformMetadataInClassFile result
                        }
                        if (JvmAnnotationNames.METADATA_VERSION_FIELD_NAME == fieldName) version else null
                    }
                }
        )
        val output = compileKotlin("source.kt", tmpdir, Arrays.asList(*additionalOptions), library)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersionJs(libraryName: String, vararg additionalOptions: String) {
        compileLibrary(K2JSCompiler(), libraryName, File(tmpdir, "library.js"), emptyList<String>())

        val library = File(tmpdir, "library.meta.js")
        library.writeText(library.readText(Charsets.UTF_8).replace(
                "(" + JsMetadataVersion.INSTANCE.toInteger() + ", ",
                "(" + JsMetadataVersion(42, 0, 0).toInteger() + ", "
        ), Charsets.UTF_8)

        val output = compileKotlin(
                K2JSCompiler(), "source.kt", File(tmpdir, "usage.js"), Arrays.asList(*additionalOptions), library
        )
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
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
            compileLibrary(compiler, libraryName, destination, emptyList<String>())
        }
        finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }

        val output: Pair<String, ExitCode>
        try {
            System.setProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, "false")
            output = compileKotlin(compiler, "source.kt", usageDestination, Arrays.asList(*additionalOptions), result)
        }
        finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }

        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    // ------------------------------------------------------------------------------

    @Throws(Exception::class)
    fun testRawTypes() {
        KotlinTestUtils.compileJavaFiles(
                listOf(File(testDataDirectory.toString() + "/library/test/A.java")),
                Arrays.asList("-d", tmpdir.path)
        )

        val outputLib = compileKotlin("library/test/lib.kt", tmpdir, tmpdir)

        val outputMain = compileKotlin("main.kt", tmpdir, tmpdir)

        KotlinTestUtils.assertEqualsToFile(
                File(testDataDirectory, "output.txt"), normalizeOutput(outputLib) + "\n" + normalizeOutput(outputMain)
        )
    }

    @Throws(Exception::class)
    fun testDuplicateObjectInBinaryAndSources() {
        val allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"))
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size)
        for (descriptor in allDescriptors) {
            assertTrue("Wrong name: " + descriptor, descriptor.name.asString() == "Lol")
            assertTrue("Should be an object: " + descriptor, isObject(descriptor))
        }
    }

    @Throws(Exception::class)
    fun testBrokenJarWithNoClassForObject() {
        val brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class")
        val allDescriptors = analyzeAndGetAllDescriptors(brokenJar)
        assertEmpty("No descriptors should be found: " + allDescriptors, allDescriptors)
    }

    @Throws(Exception::class)
    fun testSameLibraryTwiceInClasspath() {
        doTestWithTxt(compileLibrary("library-1"), compileLibrary("library-2"))
    }

    @Throws(Exception::class)
    fun testMissingEnumReferencedInAnnotationArgument() {
        doTestWithTxt(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"))
    }

    @Throws(Exception::class)
    fun testNoWarningsOnJavaKotlinInheritance() {
        // This test checks that there are no PARAMETER_NAME_CHANGED_ON_OVERRIDE or DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES
        // warnings when subclassing in Kotlin from Java binaries (in case when no parameter names are available for Java classes)

        KotlinTestUtils.compileJavaFiles(
                listOf(getTestDataFileWithExtension("java")),
                Arrays.asList("-d", tmpdir.path)
        )

        val environment = createEnvironment(listOf(tmpdir))

        val result = JvmResolveUtil.analyze(
                KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt")), environment
        )
        result.throwIfError()

        val bindingContext = result.bindingContext
        AnalyzerWithCompilerReport.reportDiagnostics(
                bindingContext.diagnostics,
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        )

        assertEquals("There should be no diagnostics", 0, Iterables.size(bindingContext.diagnostics))
    }

    @Throws(Exception::class)
    fun testIncompleteHierarchyInJava() {
        doTestBrokenJavaLibrary("library", "test/Super.class")
    }

    @Throws(Exception::class)
    fun testIncompleteHierarchyInKotlin() {
        doTestBrokenKotlinLibrary("library", "test/Super.class")
    }

    @Throws(Exception::class)
    fun testMissingDependencySimple() {
        doTestBrokenKotlinLibrary("library", "a/A.class")
    }

    @Throws(Exception::class)
    fun testMissingDependencyDifferentCases() {
        doTestBrokenKotlinLibrary("library", "a/A.class")
    }

    @Throws(Exception::class)
    fun testMissingDependencyNestedAnnotation() {
        doTestBrokenKotlinLibrary("library", "a/A\$Anno.class")
    }

    @Throws(Exception::class)
    fun testMissingDependencyConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(compileLibrary("library1"),
                                               "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class")
        val library2 = copyJarFileWithoutEntry(compileLibrary("library2"),
                                               "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class")
        val output = compileKotlin("source.kt", tmpdir, library1, library2)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
    fun testMissingDependencyJava() {
        doTestBrokenJavaLibrary("library", "test/Bar.class")
    }

    @Throws(Exception::class)
    fun testMissingDependencyJavaConflictingLibraries() {
        val library1 = deletePaths(compileJava("library1"), "test/A.class", "test/A\$Inner.class")
        val library2 = deletePaths(compileJava("library2"), "test/A.class", "test/A\$Inner.class")
        val output = compileKotlin("source.kt", tmpdir, library1, library2)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
    fun testMissingDependencyJavaNestedAnnotation() {
        doTestBrokenJavaLibrary("library", "test/A\$Anno.class")
    }

    @Throws(Exception::class)
    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        val destination = File(tmpdir, "library.jar")
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", destination, destination, tmpdir)
    }

    @Throws(Exception::class)
    fun testReleaseCompilerAgainstPreReleaseLibraryJs() {
        doTestPreReleaseKotlinLibrary(
                K2JSCompiler(), "library",
                File(tmpdir, "library.js"), File(tmpdir, "library.meta.js"),
                File(tmpdir, "usage.js")
        )
    }

    @Throws(Exception::class)
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipVersionCheck() {
        val destination = File(tmpdir, "library.jar")
        doTestPreReleaseKotlinLibrary(
                K2JVMCompiler(), "library",
                destination, destination, tmpdir,
                "-Xskip-metadata-version-check"
        )
    }

    @Throws(Exception::class)
    fun testReleaseCompilerAgainstPreReleaseLibraryJsSkipVersionCheck() {
        doTestPreReleaseKotlinLibrary(
                K2JSCompiler(), "library",
                File(tmpdir, "library.js"), File(tmpdir, "library.meta.js"),
                File(tmpdir, "usage.js"),
                "-Xskip-metadata-version-check"
        )
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersion() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null)
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersionJs() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library")
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersionBadMetadata() {
        doTestKotlinLibraryWithWrongMetadataVersion(
                "library",
                { name, value ->
                    if (JvmAnnotationNames.METADATA_DATA_FIELD_NAME == name) {
                        val strings = value as Array<String>
                        for (i in strings.indices) {
                            val bytes = strings[i].toByteArray()
                            for (j in bytes.indices) bytes[j] = bytes[j] xor 42
                            strings[i] = String(bytes)
                        }
                        return@doTestKotlinLibraryWithWrongMetadataVersion strings
                    }
                    null
                }
        )
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersionBadMetadata2() {
        doTestKotlinLibraryWithWrongMetadataVersion(
                "library",
                { name, value ->
                    if (JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME == name) {
                        return@doTestKotlinLibraryWithWrongMetadataVersion ArrayUtil.EMPTY_STRING_ARRAY
                    }
                    null
                }
        )
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersionSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, "-Xskip-metadata-version-check")
    }

    @Throws(Exception::class)
    fun testWrongMetadataVersionJsSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library", "-Xskip-metadata-version-check")
    }

    /*test source mapping generation when source info is absent*/
    @Throws(Exception::class)
    fun testInlineFunWithoutDebugInfo() {
        compileKotlin("sourceInline.kt", tmpdir)

        val inlineFunClass = File(tmpdir.absolutePath, "test/A.class")
        val cw = ClassWriter(Opcodes.ASM5)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.ASM5, cw) {
            override fun visitSource(source: String, debug: String) {
                //skip debug info
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        compileKotlin("source.kt", tmpdir, tmpdir)

        val debugInfo = Ref<String>()
        val resultFile = File(tmpdir.absolutePath, "test/B.class")
        ClassReader(resultFile.readBytes()).accept(object : ClassVisitor(Opcodes.ASM5) {
            override fun visitSource(source: String, debug: String) {
                //skip debug info
                debugInfo.set(debug)
            }
        }, 0)

        val expected = "SMAP\n" +
                       "source.kt\n" +
                       "Kotlin\n" +
                       "*S Kotlin\n" +
                       "*F\n" +
                       "+ 1 source.kt\n" +
                       "test/B\n" +
                       "*L\n" +
                       "1#1,13:1\n" +
                       "*E\n"

        if (InlineCodegenUtil.GENERATE_SMAP) {
            assertEquals(expected, debugInfo.get())
        }
        else {
            assertEquals(null, debugInfo.get())
        }
    }

    @Throws(Exception::class)
    fun testReplaceAnnotationClassWithInterface() {
        val library1 = compileLibrary("library-1")
        val usage = compileLibrary("usage", library1)
        val library2 = compileLibrary("library-2")
        doTestWithTxt(usage, library2)
    }

    @Throws(Exception::class)
    fun testProhibitNestedClassesByDollarName() {
        val library = compileLibrary("library")

        KotlinTestUtils.compileJavaFiles(
                listOf(File(testDataDirectory.toString() + "/library/test/JavaOuter.java")),
                Arrays.asList("-d", tmpdir.path)
        )

        val outputMain = compileKotlin("main.kt", tmpdir, tmpdir, library)

        KotlinTestUtils.assertEqualsToFile(
                File(testDataDirectory, "output.txt"), normalizeOutput(outputMain)
        )
    }

    fun testTypeAliasesAreInvisibleInCompatibilityMode() {
        compileKotlin("typeAliases.kt", tmpdir)

        val outputMain = compileKotlin("main.kt", tmpdir, Arrays.asList("-language-version", "1.0"), tmpdir)

        KotlinTestUtils.assertEqualsToFile(
                File(testDataDirectory, "output.txt"), normalizeOutput(outputMain)
        )
    }

    @Throws(Exception::class)
    fun testInnerClassPackageConflict() {
        compileJava("library")
        FileUtil.copy(File(testDataDirectory, "library/test/Foo/x.txt"),
                      File(tmpdir, "library/test/Foo/x.txt"))
        MockLibraryUtil.createJarFile(tmpdir, File(tmpdir, "library"), null, "library", false)
        val jarPath = File(tmpdir, "library.jar")
        val output = compileKotlin("source.kt", tmpdir, jarPath)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
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

        val output = compileKotlin("source.kt", tmpdir, library1)
        KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, "output.txt"), normalizeOutput(output))
    }

    @Throws(Exception::class)
    fun testWrongInlineTarget() {
        val library = compileLibrary("library", Arrays.asList("-jvm-target", "1.8"))

        val outputMain = compileKotlin("source.kt", tmpdir, library)

        KotlinTestUtils.assertEqualsToFile(
                File(testDataDirectory, "output.txt"), normalizeOutput(outputMain)
        )
    }

    companion object {
        private val TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/"
        private val JAVA_FILES = Pattern.compile(".*\\.java$")

        private fun copyJarFileWithoutEntry(jarPath: File, vararg entriesToDelete: String): File {
            return transformJar(jarPath, { s, bytes -> bytes }, *entriesToDelete)
        }

        private fun transformJar(
                jarPath: File,
                transformEntry: Function2<String, ByteArray, ByteArray>,
                vararg entriesToDelete: String
        ): File {
            try {
                val outputFile = File(jarPath.parentFile, FileUtil.getNameWithoutExtension(jarPath) + "-after.jar")
                val toDelete = setOf(*entriesToDelete)

                JarFile(jarPath).use { jar ->
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { output ->
                        val enumeration = jar.entries()
                        while (enumeration.hasMoreElements()) {
                            val jarEntry = enumeration.nextElement()
                            val name = jarEntry.name
                            if (toDelete.contains(name)) {
                                continue
                            }
                            val bytes = FileUtil.loadBytes(jar.getInputStream(jarEntry))
                            val newBytes = if (name.endsWith(".class")) transformEntry.invoke(name, bytes) else bytes
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
            catch (e: IOException) {
                throw rethrow(e)
            }

        }

        private fun deletePaths(library: File, vararg pathsToDelete: String): File {
            for (pathToDelete in pathsToDelete) {
                val fileToDelete = File(library, pathToDelete)
                assert(fileToDelete.delete()) { "Can't delete " + fileToDelete }
            }
            return library
        }
    }
}
