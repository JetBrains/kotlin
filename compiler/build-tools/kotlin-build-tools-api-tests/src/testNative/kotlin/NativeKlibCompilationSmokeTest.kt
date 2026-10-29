/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.nativeProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

@DisplayName("Smoke tests for Kotlin/Native klib compilation")
@TestMetadata("basic-multimodule-project")
class NativeKlibCompilationSmokeTest : BaseCompilationTest() {
    @Test
    @DisplayName("Compiles a single module into a klib in-process")
    fun testSingleModule() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        nativeProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("basic-multimodule-project/module-1")
            module1.compile(compilationConfigAction = {
                it.compilerArguments.applyCommandLineArguments(listOf("-version"))
            })
            assertTrue(module1.outputKlib.exists()) { "Expected klib at ${module1.outputKlib}" }
        }
    }

    @Test
    @DisplayName("Compiles a module depending on another klib in-process")
    fun testDependentModules() {
        val toolchain = KotlinToolchains.loadImplementation(btaClassloader)
        nativeProject(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", dependencies = listOf(module1))
            module1.compile()
            module2.compile()
            assertTrue(module2.outputKlib.exists()) { "Expected klib at ${module2.outputKlib}" }
        }
    }
}
