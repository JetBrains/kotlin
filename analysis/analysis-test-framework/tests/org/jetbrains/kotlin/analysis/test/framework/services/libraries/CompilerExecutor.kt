/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import java.nio.file.Path
import kotlin.io.path.*

internal object CompilerExecutor {
    fun compileLibrary(
        compilerKind: CompilerKind,
        sourcesPath: Path,
        options: List<String>,
        compilationErrorExpected: Boolean,
        libraryName: String,
        extraClasspath: List<String>,
    ): Path {
        val library = try {
            compile(compilerKind, sourcesPath, options, libraryName, extraClasspath)
        } catch (e: Throwable) {
            if (!compilationErrorExpected) {
                throw IllegalStateException("Unexpected compilation error while compiling library", e)
            }
            null
        }

        if (library?.exists() == true && compilationErrorExpected) {
            error("Compilation error expected but, code was compiled successfully")
        }
        if (library == null || library.notExists()) {
            throw LibraryWasNotCompiledDueToExpectedCompilationError()
        }
        return library
    }

    private fun compile(
        compilerKind: CompilerKind,
        sourcesPath: Path,
        options: List<String>,
        libraryName: String,
        extraClasspath: List<String>,
    ): Path {
        val sourceFiles = sourcesPath.toFile().walkBottomUp()
        val library = when (compilerKind) {
            CompilerKind.JVM -> sourcesPath / "$libraryName.jar"
            CompilerKind.JS -> sourcesPath / "$libraryName.klib"
        }

        when (compilerKind) {
            CompilerKind.JVM -> {
                val extraOptions = buildList {
                    addAll(options)
                    add("-XXLanguage:-${LanguageFeature.SkipStandaloneScriptsInSourceRoots.name}")
                }

                MockLibraryUtil.compileLibraryToJar(
                    sourcesPath = sourcesPath.absolutePathString(),
                    contentDir = sourcesPath.toFile(),
                    jarName = libraryName,
                    extraOptions = extraOptions,
                    assertions = JUnit5Assertions,
                    useJava11 = true,
                    extraClasspath = extraClasspath,
                )
            }
            CompilerKind.JS -> {
                val commands = buildList {
                    add(K2JSCompilerArguments::moduleName.cliArgument); add(libraryName)
                    add(K2JSCompilerArguments::outputDir.cliArgument); add(library.parent.absolutePathString())
                    add(K2JSCompilerArguments::irProduceKlibFile.cliArgument)
                    sourceFiles.mapTo(this) { it.absolutePath }
                    addAll(options)
                }
                MockLibraryUtil.runJsCompiler(commands)
            }
        }

        return library
    }

    enum class CompilerKind {
        JVM, JS
    }
}

class LibraryWasNotCompiledDueToExpectedCompilationError : SkipTestException()
