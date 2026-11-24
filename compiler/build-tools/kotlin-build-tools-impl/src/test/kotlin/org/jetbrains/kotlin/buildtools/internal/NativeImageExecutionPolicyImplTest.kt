/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    @DisplayName("Modifying a builder created from the native image process policy does not affect the original policy")
    fun testProcessPolicyToBuilderIndependence() {
        val policy = NativeImageProcessExecutionPolicyImpl(Path("dist"))
        val builder = policy.toBuilder() as NativeImageProcessExecutionPolicyImpl

        builder.distPath = Path("another-dist")

        assertEquals(Path("dist"), policy.distPath)
        assertEquals(Path("another-dist"), builder.distPath)
    }

    @Test
    @DisplayName("The native image execution policies cannot be built without the dist path")
    fun testBuildWithoutDistPathFails() {
        assertThrows<IllegalStateException> { NativeImageProcessExecutionPolicyImpl().build() }
        assertThrows<IllegalStateException> { NativeImageDaemonExecutionPolicyImpl().build() }
    }

    @Test
    @DisplayName("Building the native image execution policies with the dist path set keeps the dist path")
    fun testBuildWithDistPath() {
        val processPolicy = NativeImageProcessExecutionPolicyImpl(Path("dist")).build() as NativeImageProcessExecutionPolicyImpl
        assertEquals(Path("dist"), processPolicy.distPath)
        assertEquals(Path("dist"), processPolicy.dist.home)

        val daemonPolicy = NativeImageDaemonExecutionPolicyImpl(Path("dist")).build() as NativeImageDaemonExecutionPolicyImpl
        assertEquals(Path("dist"), daemonPolicy.distPath)
        assertEquals(Path("dist"), daemonPolicy.dist.home)
    }
}
