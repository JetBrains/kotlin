/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class DaemonExecutionPolicyDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val daemonPolicy = kotlinToolchains.daemonExecutionPolicyBuilder().build()
        assertEquals(null, daemonPolicy[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS])
        assertEquals(null, daemonPolicy[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS])
        @OptIn(DelicateBuildToolsApi::class)
        assertEquals(Path(COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH), daemonPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH])
    }
}