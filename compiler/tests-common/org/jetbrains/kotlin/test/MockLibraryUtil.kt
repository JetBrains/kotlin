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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.io.ZipUtil
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.PathUtil

import java.io.*
import java.lang.ref.SoftReference
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream

import org.junit.Assert.assertEquals

object MockLibraryUtil {

    private var compilerClassLoader = SoftReference<ClassLoader>(null)

    @JvmStatic
    fun compileLibraryToJar(
            sourcesPath: String,
            jarName: String,
            addSources: Boolean,
            isJsLibrary: Boolean,
            allowKotlinPackage: Boolean,
            vararg extraClasspath: String
    ): File {
        if (isJsLibrary) {
            return compileJsLibraryToJar(sourcesPath, jarName, addSources)
        }
        else {
            return compileLibraryToJar(sourcesPath, jarName, addSources, allowKotlinPackage, *extraClasspath)
        }
    }

    @JvmStatic
    fun compileLibraryToJar(
            sourcesPath: String,
            jarName: String,
            addSources: Boolean,
            allowKotlinPackage: Boolean,
            vararg extraClasspath: String
    ): File {
        try {
            return compileLibraryToJar(
                    sourcesPath,
                    KotlinTestUtils.tmpDir("testLibrary-" + jarName),
                    jarName,
                    addSources,
                    allowKotlinPackage,
                    *extraClasspath)
        }
        catch (e: IOException) {
            throw rethrow(e)
        }

    }

    @JvmStatic
    fun compileLibraryToJar(
            sourcesPath: String,
            contentDir: File,
            jarName: String,
            addSources: Boolean,
            allowKotlinPackage: Boolean,
            vararg extraClasspath: String
    ): File {
        try {
            val classesDir = File(contentDir, "classes")

            val srcFile = File(sourcesPath)
            val kotlinFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), srcFile)
            if (srcFile.isFile || !kotlinFiles.isEmpty()) {
                compileKotlin(sourcesPath, classesDir, allowKotlinPackage, *extraClasspath)
            }

            val javaFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), srcFile)
            if (!javaFiles.isEmpty()) {
                val classpath = ArrayList<String>()
                classpath.add(ForTestCompileRuntime.runtimeJarForTests().path)
                classpath.add(KotlinTestUtils.getAnnotationsJar().path)
                Collections.addAll(classpath, *extraClasspath)

                // Probably no kotlin files were present, so dir might not have been created after kotlin compiler
                if (classesDir.exists()) {
                    classpath.add(classesDir.path)
                }
                else {
                    FileUtil.createDirectory(classesDir)
                }

                val options = Arrays.asList(
                        "-classpath", StringUtil.join(classpath, File.pathSeparator),
                        "-d", classesDir.path
                )

                KotlinTestUtils.compileJavaFiles(javaFiles, options)
            }

            return createJarFile(contentDir, classesDir, sourcesPath, jarName, addSources)
        }
        catch (e: IOException) {
            throw rethrow(e)
        }

    }

    private fun compileJsLibraryToJar(
            sourcesPath: String,
            jarName: String,
            addSources: Boolean
    ): File {
        try {
            val contentDir = KotlinTestUtils.tmpDir("testLibrary-" + jarName)

            val outDir = File(contentDir, "out")
            val outputFile = File(outDir, jarName + ".js")
            compileKotlin2JS(sourcesPath, outputFile)

            return createJarFile(contentDir, outDir, sourcesPath, jarName, addSources)
        }
        catch (e: IOException) {
            throw rethrow(e)
        }

    }

    @Throws(IOException::class)
    fun createJarFile(contentDir: File, dirToAdd: File, sourcesPath: String, jarName: String, addSources: Boolean): File {
        val jarFile = File(contentDir, jarName + ".jar")

        val zip = ZipOutputStream(FileOutputStream(jarFile))
        ZipUtil.addDirToZipRecursively(zip, jarFile, dirToAdd, "", null, null)
        if (addSources) {
            ZipUtil.addDirToZipRecursively(zip, jarFile, File(sourcesPath), "src", null, null)
        }
        zip.close()

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
        try {
            val outStream = ByteArrayOutputStream()
            val compiler = compilerClass.newInstance()
            val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)

            val invocationResult = execMethod.invoke(compiler, PrintStream(outStream), ArrayUtil.toStringArray(args)) as Enum<*>

            assertEquals(String(outStream.toByteArray()), ExitCode.OK.name, invocationResult.name)
        }
        catch (e: Throwable) {
            throw rethrow(e)
        }

    }

    @JvmStatic
    fun compileKotlin(sourcesPath: String, outDir: File, vararg extraClasspath: String) {
        compileKotlin(sourcesPath, outDir, false, *extraClasspath)
    }

    fun compileKotlin(
            sourcesPath: String,
            outDir: File,
            allowKotlinPackage: Boolean,
            vararg extraClasspath: String
    ) {
        val classpath = ArrayList<String>()
        if (File(sourcesPath).isDirectory) {
            classpath.add(sourcesPath)
        }
        Collections.addAll(classpath, *extraClasspath)

        val args = ArrayList<String>()
        args.add(sourcesPath)
        args.add("-d")
        args.add(outDir.absolutePath)
        args.add("-classpath")
        args.add(StringUtil.join(classpath, File.pathSeparator))
        if (allowKotlinPackage) {
            args.add("-Xallow-kotlin-package")
        }

        runJvmCompiler(args)
    }

    private fun compileKotlin2JS(sourcesPath: String, outputFile: File) {
        val args = ArrayList<String>()

        args.add("-meta-info")
        args.add("-output")
        args.add(outputFile.absolutePath)

        args.add(sourcesPath)

        runJsCompiler(args)
    }

    fun compileKotlinModule(modulePath: String) {
        runJvmCompiler(Arrays.asList("-no-stdlib", "-module", modulePath))
    }

    private val compiler2JVMClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JVMCompiler::class.java.name)

    private val compiler2JSClass: Class<*>
        @Synchronized get() = loadCompilerClass(K2JSCompiler::class.java.name)

    @Synchronized private fun loadCompilerClass(compilerClassName: String): Class<*> {
        try {
            var classLoader = compilerClassLoader.get()
            if (classLoader == null) {
                classLoader = createCompilerClassLoader()
                compilerClassLoader = SoftReference<ClassLoader>(classLoader)
            }
            return classLoader.loadClass(compilerClassName)
        }
        catch (e: Throwable) {
            throw rethrow(e)
        }

    }

    @Synchronized private fun createCompilerClassLoader(): ClassLoader {
        try {
            val kotlinCompilerJar = File(PathUtil.getKotlinPathsForDistDirectory().libPath, "kotlin-compiler.jar")
            return ClassPreloadingUtils.preloadClasses(
                    listOf(kotlinCompilerJar), Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE, null, null, null
            )
        }
        catch (e: Throwable) {
            throw rethrow(e)
        }

    }
}
