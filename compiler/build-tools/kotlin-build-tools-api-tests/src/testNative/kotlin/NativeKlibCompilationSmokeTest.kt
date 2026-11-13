/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain.Companion.native
import org.jetbrains.kotlin.buildtools.api.konan.nativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.tests.compilation.model.nativeProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunAlways
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@MustRunAlways
@DisplayName("Smoke tests for Kotlin/Native klib compilation")
class NativeKlibCompilationSmokeTest : BaseCompilationTest() {
    @Test
    @TestMetadata("basic-multimodule-project")
    @DisplayName("Compiles a single module into a klib in-process")
    fun testSingleModule() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        nativeProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("basic-multimodule-project/module-1")
            module1.compile(compilationConfigAction = {
                it.compilerArguments.applyCommandLineArguments(listOf("-version"))
            }) {
                assertOutputs("basic-multimodule-project_module-1.klib")
            }
        }
    }

    @Test
    @TestMetadata("basic-multimodule-project")
    @DisplayName("Compiles a module depending on another klib in-process")
    fun testDependentModules() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        nativeProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", dependencies = listOf(module1))
            module1.compile()
            module2.compile {
                assertOutputs("basic-multimodule-project_module-2.klib")
            }
        }
    }

    @Test
    @DisplayName("Reports an error for the daemon execution policy")
    fun testDaemonPolicyIsRejected() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        // the operation fails before connecting to the daemon or reading the sources
        val operation = toolchain.native.nativeKlibCompilationOperation(
            listOf(workingDirectory.resolve("main.kt")),
            workingDirectory.resolve("out.klib"),
        )
        val logger = TestKotlinLogger()
        val result = toolchain.createBuildSession().use { session ->
            session.executeOperation(operation, toolchain.daemonExecutionPolicyBuilder().build(), logger)
        }
        assertEquals(CompilationResult.COMPILATION_ERROR, result)
        val errors = logger.logMessagesByLevel.getValue(LogLevel.ERROR)
        assertTrue(errors.any { "via the Kotlin daemon is not supported" in it }) { "Unexpected errors: $errors" }
    }
}
