/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.defaults

import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_COUNT_LIMIT
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_DIRECTORY
import org.jetbrains.kotlin.daemon.common.DEFAULT_LOG_FILE_SIZE_LIMIT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
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
        assertEquals(Path(DEFAULT_LOG_FILE_DIRECTORY), daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_PATH])
        assertEquals(DEFAULT_LOG_FILE_SIZE_LIMIT, daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_FILE_SIZE_LIMIT])
        assertEquals(DEFAULT_LOG_FILE_COUNT_LIMIT, daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_FILE_COUNT_LIMIT])
    }

    @DisplayName("KT-91113: default daemon run dir follows user.home changes")
    @Test
    @OptIn(DelicateBuildToolsApi::class)
    fun modifyingUserHomeReflectsInRunDirPath(@TempDir tempDir: Path) {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        withSystemProperties(
            USER_HOME_PROPERTY to tempDir.resolve("first").toString(),
            // the test tasks set this property, and it takes precedence over `user.home`
            "kotlin.daemon.custom.run.files.path.for.tests" to null,
        ) {
            val firstExpectedPath = Path(COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH)
            val firstPolicy = kotlinToolchains.daemonExecutionPolicyBuilder().build()
            assertEquals(firstExpectedPath, firstPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH])

            System.setProperty(USER_HOME_PROPERTY, tempDir.resolve("second").toString())
            val secondExpectedPath = Path(COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH)
            assumeTrue(firstExpectedPath != secondExpectedPath) {
                "The default daemon run dir does not depend on `user.home` in this environment"
            }

            val secondPolicy = kotlinToolchains.daemonExecutionPolicyBuilder().build()
            assertEquals(secondExpectedPath, secondPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH])
            // an already built policy is a snapshot and must not be affected
            assertEquals(firstExpectedPath, firstPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH])
        }
    }

    private fun withSystemProperties(vararg properties: Pair<String, String?>, action: () -> Unit) {
        val previousValues = properties.map { it.first to System.getProperty(it.first) }
        try {
            properties.forEach { setOrClearSystemProperty(it.first, it.second) }
            action()
        } finally {
            previousValues.forEach { setOrClearSystemProperty(it.first, it.second) }
        }
    }

    private fun setOrClearSystemProperty(name: String, value: String?) {
        if (value == null) System.clearProperty(name) else System.setProperty(name, value)
    }

    private companion object {
        const val USER_HOME_PROPERTY = "user.home"
    }
}
