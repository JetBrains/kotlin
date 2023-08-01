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
import com.intellij.util.io.ZipUtil
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.regex.Pattern

abstract class AbstractKotlinCompilerIntegrationTest : TestCaseWithTmpdir() {
    protected abstract val testDataPath: String

    protected val testDataDirectory: File
        get() = File(testDataPath, getTestName(true))

    protected fun getTestDataFileWithExtension(extension: String): File {
        return File(testDataDirectory, "${getTestName(true)}.$extension")
    }

    class JavaCompilationError : AssertionError("Java files are not compiled successfully")

    /**
     * Compiles all sources (.java and .kt) under the directory named [libraryName] to [destination].
     * [destination] should be either a path to the directory under [tmpdir], or a path to the resulting .jar file (also under [tmpdir]).
     * Kotlin sources are compiled first, and there should be no errors or warnings. Java sources are compiled next.
     *
     * @return [destination]
     */
    protected fun compileLibrary(
        libraryName: String,
        destination: File = File(tmpdir, "$libraryName.jar"),
        additionalOptions: List<String> = emptyList(),
        compileJava: (sourceDir: File, javaFiles: List<File>, outputDir: File) -> Boolean = { _, javaFiles, outputDir ->
            KotlinTestUtils.compileJavaFiles(javaFiles, listOf("-d", outputDir.path))
        },
        checkKotlinOutput: (String) -> Unit = { actual -> assertEquals(normalizeOutput("" to ExitCode.OK), actual) },
        manifest: Manifest? = null,
        extraClassPath: List<File> = emptyList()
    ): File {
        val sourceDir = File(testDataDirectory, libraryName)
        val javaFiles = FileUtil.findFilesByMask(JAVA_FILES, sourceDir)
        val kotlinFiles = FileUtil.findFilesByMask(KOTLIN_FILES, sourceDir)
        assert(javaFiles.isNotEmpty() || kotlinFiles.isNotEmpty()) { "There should be either .kt or .java files in the directory" }

        val isJar = destination.name.endsWith(".jar")

        val outputDir = if (isJar) File(tmpdir, "output-$libraryName") else destination
        if (kotlinFiles.isNotEmpty()) {
            val output = compileKotlin(libraryName, outputDir, extraClassPath, K2JVMCompiler(), additionalOptions, expectedFileName = null)
            checkKotlinOutput(normalizeOutput(output))
        }

        if (javaFiles.isNotEmpty()) {
            outputDir.mkdirs()
            if (!compileJava(sourceDir, javaFiles, outputDir)) {
                throw JavaCompilationError()
            }
        }

        if (isJar) {
            destination.delete()
            val stream =
                if (manifest != null) JarOutputStream(destination.outputStream(), manifest)
                else JarOutputStream(destination.outputStream())
            stream.use { jar ->
                ZipUtil.addDirToZipRecursively(jar, destination, outputDir, "", null, null)
            }
        } else assertNull("Manifest is ignored if destination is not a .jar file", manifest)

        return destination
    }

    /**
     * Compiles all .kt sources under the directory named [libraryName] to a file named "[libraryName].js" in [tmpdir]
     *
     * @return the path to the corresponding .meta.js file, i.e. "[libraryName].meta.js"
     */
    protected fun compileJsLibrary(
        libraryName: String,
        additionalOptions: List<String> = emptyList(),
        checkKotlinOutput: (String) -> Unit = { actual -> assertEquals(normalizeOutput("" to ExitCode.OK), actual) }
    ): File {
        val destination = File(tmpdir, libraryName)
        val output = compileKotlin(
            libraryName, destination, compiler = K2JSCompiler(), additionalOptions = additionalOptions, expectedFileName = null
        )
        checkKotlinOutput(normalizeOutput(output))
        return destination
    }

    /**
     * Compiles all .kt sources under the directory named [libraryName] to a directory named "[libraryName]" in [tmpdir]
     *
     * @return the path to the corresponding directory
     */
    protected fun compileCommonLibrary(
        libraryName: String,
        additionalOptions: List<String> = emptyList(),
        checkKotlinOutput: (String) -> Unit = { actual -> assertEquals(normalizeOutput("" to ExitCode.OK), actual) }
    ): File {
        val destination = File(tmpdir, libraryName)
        val output = compileKotlin(
            libraryName, destination, compiler = K2MetadataCompiler(), additionalOptions = additionalOptions, expectedFileName = null
        )
        checkKotlinOutput(normalizeOutput(output))
        return destination
    }

    protected fun normalizeOutput(output: Pair<String, ExitCode>): String {
        return AbstractCliTest.getNormalizedCompilerOutput(
            output.first,
            output.second,
            testDataDirectory.path,
            tmpdir.absolutePath
        ).removeFirWarning()
            .replace(FileUtil.toSystemIndependentName(tmpdir.absolutePath), "\$TMP_DIR\$")
    }

    private fun String.removeFirWarning(): String {
        return this.replace(
            "warning: language version 2.0 is experimental, there are no backwards compatibility guarantees for new language and library features\n", ""
        )
    }

    protected open fun compileKotlin(
        fileName: String,
        output: File,
        classpath: List<File> = emptyList(),
        compiler: CLICompiler<*> = K2JVMCompiler(),
        additionalOptions: List<String> = emptyList(),
        expectedFileName: String? = "output.txt",
        additionalSources: List<String> = emptyList(),
    ): Pair<String, ExitCode> {
        val args = mutableListOf<String>()
        val sourceFile = File(testDataDirectory, fileName)
        assert(sourceFile.exists()) { "Source file does not exist: ${sourceFile.absolutePath}" }
        args.add(sourceFile.path)

        additionalSources.mapTo(args) { File(testDataDirectory, it).path }

        if (compiler is K2JSCompiler) {
            args.add("-libraries")
            args.add((classpath + PathUtil.kotlinPathsForCompiler.jsStdLibJarPath).joinToString(File.pathSeparator))
            args.add("-Xir-produce-klib-dir")
            args.add("-Xir-only")
            args.add("-ir-output-dir")
            args.add(output.path)
            args.add("-ir-output-name")
            args.add("out")
        } else if (compiler is K2JVMCompiler || compiler is K2MetadataCompiler) {
            if (classpath.isNotEmpty()) {
                args.add("-classpath")
                args.add(classpath.joinToString(File.pathSeparator))
            }
            args.add("-d")
            args.add(output.path)
        } else {
            throw UnsupportedOperationException(compiler.toString())
        }

        args.addAll(additionalOptions)

        val result = AbstractCliTest.executeCompilerGrabOutput(compiler, args)
        if (expectedFileName != null) {
            KotlinTestUtils.assertEqualsToFile(File(testDataDirectory, expectedFileName), normalizeOutput(result))
        }
        return result
    }

    companion object {
        private val KOTLIN_FILES = Pattern.compile(".*\\.kt$")

        @JvmStatic
        protected val JAVA_FILES = Pattern.compile(".*\\.java$")!!
    }
}
