/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    @JvmOverloads
    fun compileJvmLibraryToJar(
            sourcesPath: String,
            jarName: String,
            addSources: Boolean = false,
            allowKotlinSources: Boolean = true,
            extraOptions: List<String> = emptyList(),
            extraClasspath: List<String> = emptyList(),
            useJava9: Boolean = false
    ): File {
        return compileLibraryToJar(
                sourcesPath, KotlinTestUtils.tmpDir("testLibrary-" + jarName), jarName, addSources,allowKotlinSources, extraOptions, extraClasspath
        , useJava9)}

    @JvmStatic
    @JvmOverloads
    fun compileJavaFilesLibraryToJar(
            sourcesPath: String,
            jarName: String,
            addSources: Boolean = false,
            extraOptions: List<String> = emptyList(),
            extraClasspath: List<String> = emptyList()
    ): File {
        return compileJvmLibraryToJar(
                sourcesPath, jarName, addSources,
                allowKotlinSources = false,
                extraClasspath = extraClasspath, extraOptions = extraOptions
        )
    }

    @JvmStatic
    @JvmOverloads
    fun compileLibraryToJar(
            sourcesPath: String,
            contentDir: File,
            jarName: String,
            addSources: Boolean = false,
            allowKotlinSources: Boolean = true,
            extraOptions: List<String> = emptyList(),
            extraClasspath: List<String> = emptyList(),
            useJava9: Boolean = false
    ): File {
        val classesDir = File(contentDir, "classes")

        val srcFile = File(sourcesPath)
        val kotlinFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), srcFile)
        if (srcFile.isFile || kotlinFiles.isNotEmpty()) {
            Assert.assertTrue("Only java files are expected", allowKotlinSources)
            compileKotlin(sourcesPath, classesDir, extraOptions, *extraClasspath.toTypedArray())
        }

        val javaFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), srcFile)
        if (javaFiles.isNotEmpty()) {
            val classpath = mutableListOf<String>()
            classpath += ForTestCompileRuntime.runtimeJarForTests().path
            classpath += KotlinTestUtils.getAnnotationsJar().path
            classpath += extraClasspath

            // Probably no kotlin files were present, so dir might not have been created after kotlin compiler
            if (classesDir.exists()) {
                classpath += classesDir.path
            }
            else {
                FileUtil.createDirectory(classesDir)
            }

            val options = listOf(
                    "-classpath", classpath.joinToString(File.pathSeparator),
                    "-d", classesDir.path
            )

            val compile =
                    if (useJava9) KotlinTestUtils::compileJavaFilesExternallyWithJava9
                    else KotlinTestUtils::compileJavaFiles

            val success = compile(javaFiles, options)
            if (!success) {
                throw AssertionError("Java files are not compiled successfully")
            }
        }

        return createJarFile(contentDir, classesDir, jarName, sourcesPath.takeIf { addSources })
    }

    @JvmStatic
    fun compileJsLibraryToJar(sourcesPath: String, jarName: String, addSources: Boolean): File {
        val contentDir = KotlinTestUtils.tmpDir("testLibrary-" + jarName)

        val outDir = File(contentDir, "out")
        val outputFile = File(outDir, jarName + ".js")
        compileKotlin2JS(sourcesPath, outputFile)

        return createJarFile(contentDir, outDir, jarName, sourcesPath.takeIf { addSources })
    }

    fun createJarFile(contentDir: File, dirToAdd: File, jarName: String, sourcesPath: String? = null): File {
        val jarFile = File(contentDir, jarName + ".jar")

        ZipOutputStream(FileOutputStream(jarFile)).use { zip ->
            ZipUtil.addDirToZipRecursively(zip, jarFile, dirToAdd, "", null, null)
            if (sourcesPath != null) {
                ZipUtil.addDirToZipRecursively(zip, jarFile, File(sourcesPath), "src", null, null)
            }
        }

        return jarFile
    }

    private fun runJvmCompiler(args: List<String>) {
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
        assertEquals(String(outStream.toByteArray()), ExitCode.OK.name, invocationResult.name)
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

    private fun compileKotlin2JS(sourcesPath: String, outputFile: File) {
        runJsCompiler(listOf("-meta-info", "-output", outputFile.absolutePath, sourcesPath))
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
