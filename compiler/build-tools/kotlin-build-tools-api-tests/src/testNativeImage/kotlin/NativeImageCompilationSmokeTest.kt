/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.writeText

@DisplayName("Smoke tests for the native image execution policies")
class NativeImageCompilationSmokeTest : BaseCompilationTest() {
    val nativeImageDistPath: Path = ForTestCompileRuntime.kotlinNativeImageDistForTests().toPath()

    @Test
    @DisplayName("Compiles a module using the native image process execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testProcessPolicyCompilation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val policy = kotlinToolchains.createNativeImageProcessExecutionPolicy(nativeImageDistPath)

        jvmProject(kotlinToolchains to policy) {
            val module = module("basic-multimodule-project/module-1")
            module.compile {
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
        }
    }

    @Test
    @DisplayName("Compiles a project with spaces in its path using the native image process execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testProcessPolicyCompilationWithSpacesInProjectPath() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val policy = kotlinToolchains.createNativeImageProcessExecutionPolicy(nativeImageDistPath)

        JvmProject(kotlinToolchains, policy, workingDirectory.resolve("project with spaces")).use { project ->
            val module = project.module("basic-multimodule-project/module-1")
            module.compile {
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
        }
    }

    @Test
    @DisplayName("Reports compilation errors through the logger using the native image process execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testProcessPolicyCompilationError() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val policy = kotlinToolchains.createNativeImageProcessExecutionPolicy(nativeImageDistPath)

        jvmProject(kotlinToolchains to policy) {
            val module = module("basic-multimodule-project/module-1")
            module.sourcesDirectory.resolve("bar.kt").writeText("aaaa")
            module.compile {
                expectFailWithError(".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())
            }
        }
    }

    @Test
    @DisplayName("Compiles a module using the native image daemon execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testDaemonPolicyCompilation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                }
            }
        }
    }

    @Test
    @DisplayName("Reports compilation errors through the logger using the native image daemon execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testDaemonPolicyCompilationError() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.sourcesDirectory.resolve("bar.kt").writeText("aaaa")
                module.compile {
                    expectFailWithError(".*bar\\.kt:1:1 Syntax error: Expecting a top level declaration.*".toRegex())
                }
            }
        }
    }
}
