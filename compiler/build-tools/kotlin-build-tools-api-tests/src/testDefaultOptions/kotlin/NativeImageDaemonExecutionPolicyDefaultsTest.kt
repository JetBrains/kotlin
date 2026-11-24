/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.nativeImageDaemonExecutionPolicy
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_COUNT_LIMIT
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_DIRECTORY
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_SIZE_LIMIT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path

class NativeImageDaemonExecutionPolicyDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val daemonPolicy = kotlinToolchains.nativeImageDaemonExecutionPolicy {
            this[ExecutionPolicy.NativeImageDaemon.DIST_PATH] = Path("native-image-dist")
        }
        assertEquals(Path("native-image-dist"), daemonPolicy[ExecutionPolicy.NativeImageDaemon.DIST_PATH])
        assertEquals(null, daemonPolicy[ExecutionPolicy.NativeImageDaemon.SHUTDOWN_DELAY_MILLIS])
        assertEquals(Path(COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH), daemonPolicy[ExecutionPolicy.NativeImageDaemon.DAEMON_RUN_DIR_PATH])
        assertEquals(Path(DEFAULT_LOG_FILE_DIRECTORY), daemonPolicy[ExecutionPolicy.NativeImageDaemon.LOGS_PATH])
        assertEquals(DEFAULT_LOG_FILE_SIZE_LIMIT, daemonPolicy[ExecutionPolicy.NativeImageDaemon.LOGS_FILE_SIZE_LIMIT])
        assertEquals(DEFAULT_LOG_FILE_COUNT_LIMIT, daemonPolicy[ExecutionPolicy.NativeImageDaemon.LOGS_FILE_COUNT_LIMIT])
    }

    @Test
    fun testDistPathHasNoDefault() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val builder = kotlinToolchains.nativeImageDaemonExecutionPolicyBuilder()
        assertThrows<IllegalStateException> { builder[ExecutionPolicy.NativeImageDaemon.DIST_PATH] }
        assertThrows<IllegalStateException> { builder.build() }
    }
}
