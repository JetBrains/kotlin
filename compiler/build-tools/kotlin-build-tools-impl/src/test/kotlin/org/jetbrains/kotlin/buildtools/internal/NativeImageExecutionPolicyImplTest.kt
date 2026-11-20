/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class NativeImageExecutionPolicyImplTest {

    @Test
    @DisplayName("The native image execution policies compare by all their options")
    fun testPoliciesEquality() {
        val distPath = Path("dist")
        val processPolicy = NativeImageProcessExecutionPolicyImpl(distPath)
        assertEquals(distPath, processPolicy.distPath)
        assertEquals(processPolicy, NativeImageProcessExecutionPolicyImpl(distPath))
        assertNotEquals(processPolicy, NativeImageProcessExecutionPolicyImpl(Path("another-dist")))

        val daemonPolicy = NativeImageDaemonExecutionPolicyImpl(distPath)
        assertEquals(daemonPolicy, NativeImageDaemonExecutionPolicyImpl(distPath))
        assertEquals(daemonPolicy, daemonPolicy.deepCopy())

        assertNotEquals(daemonPolicy, NativeImageDaemonExecutionPolicyImpl(Path("another-dist")))
        assertNotEquals(daemonPolicy, daemonPolicy.deepCopy().apply { daemonRunDirPath = Path("another-run-dir") })
        assertNotEquals(daemonPolicy, daemonPolicy.deepCopy().apply { shutdownDelayMillis = 42 })
    }

    @Test
    @DisplayName("Modifying a deep copy of the native image daemon policy does not affect the original policy")
    fun testDeepCopyIndependence() {
        val policy = NativeImageDaemonExecutionPolicyImpl(Path("dist")).apply {
            shutdownDelayMillis = 42
            daemonRunDirPath = Path("run")
            logsPath = Path("logs")
        }
        val copy = policy.deepCopy()

        copy.shutdownDelayMillis = null
        copy.daemonRunDirPath = Path("another-run")
        copy.logsPath = Path("another-logs")

        assertEquals(42L, policy.shutdownDelayMillis)
        assertEquals(Path("run"), policy.daemonRunDirPath)
        assertEquals(Path("logs"), policy.logsPath)
    }
}
