/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import org.jetbrains.kotlin.test.KtAssert.assertTrue
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass

object MockLibraryUtil {
    private var compilerClassLoader = SoftReference<ClassLoader>(null)

    @JvmStatic
    fun compileJvmLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        useJava11: Boolean = false,
        assertions: Assertions
    ): File {
        return compileLibraryToJar(
            sourcesPath,
            KtTestUtil.tmpDirForReusableFolder("testLibrary-$jarName"),
            jarName,
            addSources,
            allowKotlinSources,
            extraOptions,
            extraClasspath,
            extraModulepath,
            useJava11,
            assertions
        )
    }

    @JvmStatic
    fun compileJavaFilesLibraryToJar(
        sourcesPath: String,
        jarName: String,
        addSources: Boolean = false,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        assertions: Assertions,
        useJava11: Boolean = false
    ): File {
        return compileJvmLibraryToJar(
            sourcesPath, jarName, addSources,
            allowKotlinSources = false,
            extraOptions,
            extraClasspath,
            extraModulepath,
            useJava11,
            assertions
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @JvmStatic
    fun compileLibraryToJar(
        sourcesPath: String,
        contentDir: File,
        jarName: String,
        addSources: Boolean = false,
        allowKotlinSources: Boolean = true,
        extraOptions: List<String> = emptyList(),
        extraClasspath: List<String> = emptyList(),
        extraModulepath: List<String> = emptyList(),
        useJava11: Boolean = false,
        assertions: Assertions
    ): File {
        assertTrue("Module path can be used only for compilation using javac 9 and higher", useJava11 || extraModulepath.isEmpty())

        val classesDir = File(contentDir, "classes")

        val srcFile = File(sourcesPath)
        val kotlinFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), srcFile)
        if (srcFile.isFile || kotlinFiles.isNotEmpty()) {
            assertTrue("Only java files are expected", allowKotlinSources)
            compileKotlin(sourcesPath, classesDir, extraOptions, *extraClasspath.toTypedArray())
        }

        val javaFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), srcFile)
        if (javaFiles.isNotEmpty()) {
            val classpath = mutableListOf<String>()
            classpath += ForTestCompileRuntime.runtimeJarForTests().path
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
            }

            val compile =
                if (useJava11) ::compileJavaFilesExternallyWithJava11
                else { files, opts -> compileJavaFiles(files, opts, assertions = assertions) }

            val success = compile(javaFiles, options)
            if (!success) {
                throw AssertionError("Java files are not compiled successfully")
            }
        }

        return createJarFile(contentDir, classesDir, jarName, sourcesPath.takeIf { addSources })
    }

    @JvmStatic
    fun compileJsLibraryToJar(sourcesPath: String, jarName: String, addSources: Boolean, extraOptions: List<String> = emptyList()): File {
        val contentDir = KtTestUtil.tmpDirForReusableFolder("testLibrary-$jarName")

        val outDir = File(contentDir, "out")
        val outputFile = File(outDir, "$jarName.js")
        compileKotlin2JS(sourcesPath, outputFile, extraOptions)

        return createJarFile(contentDir, outDir, jarName, sourcesPath.takeIf { addSources })
    }

    @JvmStatic
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
        runCompiler(compiler2JVMClass, args)
    }

    private fun runJsCompiler(args: List<String>) {
        runCompiler(compiler2JSClass, args)
    }

    // Runs compiler in custom class loader to avoid effects caused by replacing Application with another one created in compiler.
    private fun runCompiler(compilerClass: Class<*>, args: List<String>) {
        val outStream = ByteArrayOutputStream()
        val compiler = compilerClass.newInstance()
        val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        val invocationResult = execMethod.invoke(compiler, PrintStream(outStream), args.toTypedArray()) as Enum<*>
        KtAssert.assertEquals(String(outStream.toByteArray()), ExitCode.OK.name, invocationResult.name)
    }

    @JvmStatic
    @JvmOverloads
    fun compileKotlin(
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
            "-d", outDir.absolutePath,
            "-classpath", classpath.joinToString(File.pathSeparator)
        ) + extraOptions

        runJvmCompiler(args)
    }

    private fun compileKotlin2JS(sourcesPath: String, outputFile: File, extraOptions: List<String>) {
        runJsCompiler(listOf("-meta-info", "-output", outputFile.absolutePath, sourcesPath, *extraOptions.toTypedArray()))
    }

    fun compileKotlinModule(buildFilePath: String) {
        runJvmCompiler(listOf("-no-stdlib", "-Xbuild-file", buildFilePath))
    }

    private val compiler2JVMClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JVMCompiler::class)

    private val compiler2JSClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JSCompiler::class)

    @Synchronized
    private fun loadCompilerClass(compilerClass: KClass<out CLICompiler<*>>): Class<*> {
        val classLoader = compilerClassLoader.get() ?: createCompilerClassLoader().also { classLoader ->
            compilerClassLoader = SoftReference<ClassLoader>(classLoader)
        }
        return classLoader.loadClass(compilerClass.java.name)
    }

    @Synchronized
    private fun createCompilerClassLoader(): ClassLoader {
        return ClassPreloadingUtils.preloadClasses(
            listOf(PathUtil.kotlinPathsForDistDirectory.compilerPath),
            Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE, null, null
        )
    }
}
