/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DaemonSessionsTest : BaseCompilationTest() {

    @Test
    @DisplayName("Only one session is opened for the same Daemon options")
    fun testDaemonSingleSessionIsOpen() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertDaemonWasCreated(true)
                }
                module.compile {
                    assertDaemonWasCreated(false)
                }
                // also run with a separate, but equal instance of daemonPolicy
                module.compile(daemonPolicy.toBuilder().build()) {
                    assertDaemonWasCreated(false)
                }
            }
        }
    }


    @Test
    @DisplayName("Two sessions are opened for different Daemon options")
    fun testDaemonSeparateSessionsAreOpen() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertDaemonWasCreated(true)
                }
                module.compile(daemonPolicy.toBuilder().apply {
                    this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 5L
                }.build()) {
                    assertDaemonWasCreated(true)
                }
            }
        }
    }

    private fun CompilationOutcome.assertDaemonWasCreated(wasCreated: Boolean) {
        assertLogContainsSubstringExactlyTimes(DEBUG, "successfully leased a compile session", if (wasCreated) 1 else 0)
    }
}
