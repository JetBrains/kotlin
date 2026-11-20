/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path

@DisplayName("Sessions of the native image daemon execution policy")
class NativeImageDaemonSessionsTest : BaseCompilationTest() {
    val nativeImageDistPath: Path = ForTestCompileRuntime.kotlinNativeImageDistForTests().toPath()

    @Test
    @DisplayName("Only one session is opened for the same native image daemon options")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testSingleSessionIsOpen() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertNativeImageDaemonConnectionWasCreated(true)
                }
                module.compile {
                    assertNativeImageDaemonConnectionWasCreated(false)
                }
                // also run with a separate, but equal instance of the daemon policy
                module.compile(daemonPolicy.toBuilder().build()) {
                    assertNativeImageDaemonConnectionWasCreated(false)
                }
            }
        }
    }

    @Test
    @DisplayName("Two sessions are opened for different native image daemon options")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testSeparateSessionsAreOpen() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertNativeImageDaemonConnectionWasCreated(true)
                }
                module.compile(daemonPolicy.toBuilder().apply {
                    this[ExecutionPolicy.NativeImageDaemon.SHUTDOWN_DELAY_MILLIS] = 5L
                }.build()) {
                    assertNativeImageDaemonConnectionWasCreated(true)
                }
            }
        }
    }

    private fun CompilationOutcome.assertNativeImageDaemonConnectionWasCreated(wasCreated: Boolean) {
        assertLogContainsSubstringExactlyTimes(LogLevel.DEBUG, "successfully leased a compile session", if (wasCreated) 1 else 0)
    }
}
