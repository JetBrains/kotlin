/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.KtAssert.assertTrue
import org.jetbrains.kotlin.test.MockLibraryUtil.compileKotlinSources
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream

val kotlinPathsForDistDirectoryForTestsOrNull: KotlinPaths?
    get() = System.getProperty("jps.kotlin.home")?.let(::File)?.let(::KotlinPathsFromHomeDir)
val PathUtil.kotlinPathsForDistDirectoryForTests: KotlinPaths
    get() = kotlinPathsForDistDirectoryForTestsOrNull ?: kotlinPathsForDistDirectory

object MockLibraryUtil {
    /**
     * The method is left for compatibility with the old JPS artifacts.
     * Don't use it anywhere other than in the JPS tests – use [compileKotlinSources] instead.
     *
     * JPS tests are run in the IDE with mixed classpath. The Kotlin JPS plugin itself and its tests come from artifacts of the stable
     * Kotlin version (the "default" compiler bundled in the Kotlin IDE plugin), while common compiler components are taken from a more
     * later 'kt-master'. Such an inconsistency makes it easy to introduce occasional ABI breakages that are only discovered during the
     * consequent 'kt-master' merge.
     *
     * As a long-term solution, there should appear a binary checker (KT-84534), or, alternatively, JPS tests themselves should be run
     * in the Kotlin repository (KT-84535).
     */
    @JvmStatic
    @JvmOverloads
    @Deprecated("Use 'compileKotlinSources()' instead", level = DeprecationLevel.HIDDEN)
    fun compileKotlin(
        sourcesPath: String,
        outDir: File,
        extraOptions: List<String> = emptyList(),
        vararg extraClasspath: String,
    ) {
        compileKotlinSources(sourcesPath, outDir, extraOptions, *extraClasspath)
    }

    fun compileJvmLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraJavacOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        useJava11: Boolean = false,
    ): File {
        return compileLibraryToJar(
            sourcesPath,
            KtTestUtil.tmpDirForReusableFolder("testLibrary-$jarName"),
            jarName,
            addSources,
            allowKotlinSources,
            extraOptions,
            extraJavacOptions,
            extraClasspath,
            extraModulepath,
            useJava11,
        )
    }

    fun compileJavaFilesLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        extraOptions: List<String> = emptyList(),
        extraJavacOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        assertions: Assertions,
        useJava11: Boolean = false
    ): File {
        return compileJvmLibraryToJar(
            sourcesPath, jarName, addSources,
            allowKotlinSources = false,
            extraOptions,
            extraJavacOptions,
            extraClasspath,
            extraModulepath,
            useJava11,
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun compileLibraryToJar(
        sourcesPath: String,
        contentDir: File,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraJavacOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        useJava11: Boolean = false,
    ): File {
        assertTrue("Module path can be used only for compilation using javac 9 and higher", useJava11 || extraModulepath.isEmpty())

        val classesDir = File(contentDir, "classes")

        val srcFile = File(sourcesPath)
        val kotlinFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), srcFile)
        if (srcFile.isFile || kotlinFiles.isNotEmpty()) {
            assertTrue("Only java files are expected", allowKotlinSources)
            compileKotlinSources(sourcesPath, classesDir, extraOptions, *extraClasspath.toTypedArray())
        }

        val javaFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), srcFile)
        if (javaFiles.isNotEmpty()) {
            val classpath = mutableListOf<String>()
            classpath += kotlinPathsForDistDirectoryForTestsOrNull?.stdlibPath?.path
                ?: ForTestCompileRuntime.runtimeJarForTests().path
            classpath += extraClasspath

            // Probably no kotlin files were present, so dir might not have been created after kotlin compiler
            if (classesDir.exists()) {
                classpath += classesDir.path
            } else {
                FileUtil.createDirectory(classesDir)
            }

            val options = buildList {
                add("-classpath")
                add(classpath.joinToString(File.pathSeparator))
                add("-d")
                add(classesDir.path)

                if (useJava11 && extraModulepath.isNotEmpty()) {
                    add("--module-path")
                    add(extraModulepath.joinToString(File.pathSeparator))
                }
                add("-encoding")
                add("utf8")
                addAll(extraJavacOptions)
            }

            val jdkHome = if (useJava11) KtTestUtil.getJdk11Home() else null
            compileJavaFiles(javaFiles, options, jdkHome).assertSuccessful()
        }

        return createJarFile(contentDir, classesDir, jarName, sourcesPath.takeIf { addSources })
    }

    fun createJarFile(contentDir: File, dirToAdd: File, jarName: String, sourcesPath: String? = null): File {
        val jarFile = File(contentDir, "$jarName.jar")

        ZipOutputStream(FileOutputStream(jarFile)).use { zip ->
            ZipUtil.addDirToZipRecursively(zip, jarFile, dirToAdd, "", null, null)
            if (sourcesPath != null) {
                ZipUtil.addDirToZipRecursively(zip, jarFile, File(sourcesPath), "src", null, null)
            }
        }

        return jarFile
    }

    fun runJvmCompiler(args: List<String>) {
        runCompiler(K2JVMCompiler()::exec, args)
    }

    fun runJsCompiler(args: List<String>) {
        runCompiler(K2JSCompiler()::exec, args)
    }

    fun runMetadataCompiler(args: List<String>) {
        runCompiler(KotlinMetadataCompiler()::exec, args)
    }

    private fun runCompiler(execMethod: (PrintStream, Array<String>) -> ExitCode, args: List<String>) {
        val outStream = ByteArrayOutputStream()
        val invocationResult = execMethod.invoke(PrintStream(outStream), args.toTypedArray())
        KtAssert.assertEquals(String(outStream.toByteArray()), ExitCode.OK, invocationResult)
    }

    fun compileKotlinSources(
        sourcesPath: String,
        outDir: File,
        extraOptions: List<String> = emptyList(),
        vararg extraClasspath: String
    ) {
        val classpath = mutableListOf<String>()
        if (File(sourcesPath).isDirectory) {
            classpath += sourcesPath
        }
        classpath += extraClasspath

        val args = mutableListOf(
            sourcesPath,
            K2JVMCompilerArguments::destination.cliArgument, outDir.absolutePath,
            K2JVMCompilerArguments::classpath.cliArgument, classpath.joinToString(File.pathSeparator)
        ) + extraOptions

        runJvmCompiler(args)
    }

    fun compileKotlinModule(buildFilePath: String) {
        runJvmCompiler(listOf(K2JVMCompilerArguments::noStdlib.cliArgument, K2JVMCompilerArguments::buildFile.cliArgument, buildFilePath))
    }
}
