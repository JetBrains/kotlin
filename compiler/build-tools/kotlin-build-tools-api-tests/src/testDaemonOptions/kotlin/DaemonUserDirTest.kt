/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

class DaemonUserDirTest : BaseCompilationTest() {

    @Test
    @DisplayName("Compilation works on daemon with a different user directory")
    fun testCompilationOnDaemonWithDifferentUserDir() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val newUserDir = Path(".").listDirectoryEntries().first { it.isDirectory() }
        runSingleShotDaemonTest(kotlinToolchains, additionalDaemonConfiguration = {
            this[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = listOf(
                "Duser.dir=${newUserDir.absolute()}"
            )
        }) { daemonPolicy, daemonRunPath ->
            jvmProject(kotlinToolchains, daemonPolicy) {
                val module1 = module("basic-multimodule-project/module-1")
                with(module1) {
                    val allowedExtensions = setOf("kt", "kts", "java")
                    val relativizedSources =
                        sourcesDirectory.walk().filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                            .toList().relativeToUserDir()
                    val compilationOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(
                        relativizedSources,
                        outputDirectory.relativeToUserDir()
                    )
                    moduleCompilationConfigAction(compilationOperation)
                    compilationOperation.compilerArguments[NO_REFLECT] = true
                    compilationOperation.compilerArguments[NO_STDLIB] = true
                    compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
                    compilationOperation.compilerArguments[MODULE_NAME] = moduleName
                    val operation = compilationOperation.build()
                    val result = kotlinToolchains.createBuildSession().use {
                        it.executeOperation(operation, daemonPolicy)
                    }
                    assertEquals(CompilationResult.COMPILATION_SUCCESS, result)
                }
            }
        }
    }

    private fun Path.relativeToUserDir(): Path = relativeTo(Path(".").absolute())
    private fun List<Path>.relativeToUserDir(): List<Path> = map { it.relativeToUserDir() }
}


