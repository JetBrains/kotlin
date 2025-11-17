/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.KtAssert.fail
import java.io.File
import java.io.PrintStream
import java.lang.ref.SoftReference
import java.net.URL
import java.net.URLClassLoader

/**
 * An entry point to call a custom Kotlin/JS, Kotlin/Wasm or Kotlin/Native compiler inside an isolated class loader.
 *
 * Note: The class loader is cached to be easily reused in all later calls without reloading the class path.
 * Yet it is cached as a [SoftReference] to allow GC in the case of a need.
 */
class CustomCompiler(
    private val compilerClassPath: List<URL>,
    private val className: String,
    private val methodName: String,
) {
    private var isolatedClassLoaderSoftRef: SoftReference<URLClassLoader>? = null

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaderSoftRef?.get() ?: synchronized(this) {
        isolatedClassLoaderSoftRef?.get() ?: run {
            val isolatedClassLoader = URLClassLoader(compilerClassPath.toTypedArray(), null)
            isolatedClassLoader.setDefaultAssertionStatus(true)
            isolatedClassLoaderSoftRef = SoftReference(isolatedClassLoader)
            isolatedClassLoader
        }
    }

    fun callCompiler(output: PrintStream, vararg args: List<String>?): ExitCode {
        val allArgs = args.flatMap { it.orEmpty() }.toTypedArray()
        return callCompiler(output, *allArgs)
    }

    fun callCompiler(output: PrintStream, vararg args: String): ExitCode {
        val isolatedClassLoader = getIsolatedClassLoader()

        val compilerClass = Class.forName(className, true, isolatedClassLoader)
        val entryPoint = compilerClass.getMethod(
            methodName,
            PrintStream::class.java,
            Array<String>::class.java
        )

        val compilerInstance = compilerClass.getDeclaredConstructor().newInstance()
        val result = entryPoint.invoke(compilerInstance, output, args)

        return ExitCode.valueOf(result.toString())
    }
}

sealed interface CustomCompilerArtifacts {
    val version: String

    /**
     * The classpath of the custom compiler.
     */
    val compilerClassPath: List<URL>
    val compilerDist: File?

    /**
     * Resolves the '$baseName-$version.$extension' runtime dependency artifact, where $extension is one of the passed [extensions].
     */
    fun runtimeDependency(baseName: String, vararg extensions: String): File

    private class Resolvable(
        override val version: String,
        override val compilerClassPath: List<URL>,
        private val runtimeDependencies: List<File>,
        override val compilerDist: File? = null,
    ) : CustomCompilerArtifacts {
        override fun runtimeDependency(baseName: String, vararg extensions: String): File {
            val candidates = runtimeDependencies.filter { file ->
                file.isFile && file.extension in extensions && file.nameWithoutExtension == "$baseName-$version"
            }

            return when (candidates.size) {
                0 -> fail("Artifact $baseName is not found.")
                1 -> candidates.first()
                else -> fail("More than one $baseName artifact is found: $candidates")
            }
        }
    }

    class Unresolvable(val reason: String) : CustomCompilerArtifacts {
        override val version get() = fail(reason)
        override val compilerClassPath get() = fail(reason)
        override val compilerDist get() = fail(reason)
        override fun runtimeDependency(baseName: String, vararg extensions: String) = fail(reason)
    }

    companion object {
        fun create(
            compilerClassPathPropertyName: String,
            runtimeDependenciesPropertyName: String?, // After OSIP-740, make it non-nullable to always provide stdlib
            versionPropertyName: String,
            compilerDistPropertyName: String? = null,
        ): CustomCompilerArtifacts {
            val version: String = readProperty(versionPropertyName)
                ?: return propertyNotFound(versionPropertyName)

            val compilerClassPath: List<URL> = readProperty(compilerClassPathPropertyName)
                ?.split(File.pathSeparatorChar)
                ?.map { File(it).toURI().toURL() }
                ?: return propertyNotFound(compilerClassPathPropertyName)

            val runtimeDependencies = runtimeDependenciesPropertyName?.let {
                readProperty(it)
                    ?.split(File.pathSeparatorChar)
                    ?.map(::File)
                    ?: return propertyNotFound(runtimeDependenciesPropertyName)
            } ?: emptyList()

            val compilerDist = compilerDistPropertyName?.let {
                File(readProperty(it) ?: return propertyNotFound(it))
            }

            return Resolvable(version, compilerClassPath, runtimeDependencies, compilerDist)
        }

        fun create(
            version: LanguageVersion,
            compilerDist: File,
            compilerClassPath: List<File>,
        ): CustomCompilerArtifacts =
            Resolvable(version.versionString, compilerClassPath.map { it.toURI().toURL() }, runtimeDependencies = emptyList(), compilerDist)

        fun readProperty(propertyName: String): String? =
            System.getProperty(propertyName)?.trim(Char::isWhitespace)?.takeIf(String::isNotEmpty)

        fun propertyNotFound(propertyName: String): Unresolvable = Unresolvable(
            "The Gradle property \"$propertyName\" is not specified. " +
                    "Please check the README.md in the root of the `klib-compatibility` project."
        )
    }
}
