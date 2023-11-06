/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.notExists

object CompilerExecutor {
    fun compileLibrary(sourcesPath: Path, options: List<String>, compilationErrorExpected: Boolean): Path {
        val library = sourcesPath / "library.jar"
        val sourceFiles = sourcesPath.toFile().walkBottomUp()
        val commands = buildList {
            sourceFiles.mapTo(this) { it.absolutePath }
            addAll(options)
            add("-d")
            add(library.absolutePathString())
            add("-XXLanguage:-SkipStandaloneScriptsInSourceRoots")
        }
        try {
            MockLibraryUtil.runJvmCompiler(commands)
        } catch (e: Throwable) {
            if (!compilationErrorExpected) {
                throw IllegalStateException("Unexpected compilation error while compiling library", e)
            }
        }

        if (library.exists() && compilationErrorExpected) {
            error("Compilation error expected but, code was compiled successfully")
        }
        if (library.notExists()) {
            throw LibraryWasNotCompiledDueToExpectedCompilationError()
        }
        return library
    }

    fun parseCompilerOptionsFromTestdata(module: TestModule): List<String> = buildList {
        module.directives[LanguageSettingsDirectives.API_VERSION].firstOrNull()?.let { apiVersion ->
            addAll(listOf(CommonCompilerArguments::apiVersion.cliArgument, apiVersion.versionString))
        }

        module.directives[LanguageSettingsDirectives.LANGUAGE].firstOrNull()?.let {
            add("-XXLanguage:$it")
        }

        if (LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives) {
            add(CommonCompilerArguments::allowKotlinPackage.cliArgument)
        }

        module.directives[JvmEnvironmentConfigurationDirectives.JVM_TARGET].firstOrNull()?.let { jvmTarget ->
            addAll(listOf(K2JVMCompilerArguments::jvmTarget.cliArgument, jvmTarget.description))

            val jdkHome = when {
                jvmTarget <= JvmTarget.JVM_1_8 -> KtTestUtil.getJdk8Home()
                jvmTarget <= JvmTarget.JVM_11 -> KtTestUtil.getJdk11Home()
                jvmTarget <= JvmTarget.JVM_17 -> KtTestUtil.getJdk17Home()
                jvmTarget <= JvmTarget.JVM_21 -> KtTestUtil.getJdk21Home()
                else -> error("JDK for $jvmTarget is not found")
            }

            addAll(listOf(K2JVMCompilerArguments::jdkHome.cliArgument, jdkHome.toString()))
        }

        addAll(module.directives[Directives.COMPILER_ARGUMENTS])
    }

    object Directives : SimpleDirectivesContainer() {
        val COMPILER_ARGUMENTS by stringDirective("List of additional compiler arguments")
        val COMPILATION_ERRORS by directive("Is compilation errors expected in the file")
    }
}

class LibraryWasNotCompiledDueToExpectedCompilationError : SkipTestException()
