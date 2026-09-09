/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2PlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectWithPolicyCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.junit.jupiter.api.DisplayName

class DaemonSessionsTest : BaseCompilationTest() {

    @BtaV2PlatformAgnosticCompilationTest
    @DisplayName("Only one session is opened for the same Daemon options")
    fun testDaemonSingleSessionIsOpen(project: ProjectWithPolicyCreator) {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains) { daemonPolicy, _ ->
            project(daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertDaemonConnectionWasCreated(true)
                }
                module.compile {
                    assertDaemonConnectionWasCreated(false)
                }
                // also run with a separate, but equal instance of daemonPolicy
                module.compile(daemonPolicy.toBuilder().build()) {
                    assertDaemonConnectionWasCreated(false)
                }
            }
        }
    }


    @BtaV2PlatformAgnosticCompilationTest
    @DisplayName("Two sessions are opened for different Daemon options")
    fun testDaemonSeparateSessionsAreOpen(project: ProjectWithPolicyCreator) {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotDaemonTest(kotlinToolchains) { daemonPolicy, _ ->
            project(daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                module.compile {
                    assertDaemonConnectionWasCreated(true)
                }
                module.compile(daemonPolicy.toBuilder().apply {
                    this[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 5L
                }.build()) {
                    assertDaemonConnectionWasCreated(true)
                }
            }
        }
    }

    private fun CompilationOutcome.assertDaemonConnectionWasCreated(wasCreated: Boolean) {
        assertLogContainsSubstringExactlyTimes(DEBUG, "successfully leased a compile session", if (wasCreated) 1 else 0)
    }
}
