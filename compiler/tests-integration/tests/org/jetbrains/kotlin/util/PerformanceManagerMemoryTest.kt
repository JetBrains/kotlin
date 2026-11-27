/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import jdk.jfr.FlightRecorder
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PerformanceManagerMemoryTest {
    private val allocationSize = 64 * 1024 * 1024

    @Volatile
    private var sink: Any? = null

    private fun allocateAndCollect() {
        sink = ByteArray(allocationSize)
        @Suppress("ExplicitGarbageCollectionCall")
        System.gc()
        sink = null
    }

    @Test
    fun memoryOfPhasesAndDynamicPhasesIsReported() {
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }
        perfManager.measureCompilation().use {
            perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
                repeat(2) {
                    perfManager.tryMeasureDynamicPhaseTime("Per file", PhaseType.Backend, ::allocateAndCollect)
                }
            }
        }

        val memory = perfManager.unitStats.memoryStats!!.associateBy { it.dynamicPhase }
        assertEquals(setOf(null, "Per file"), memory.keys)
        val dynamicPhase = memory.getValue("Per file")
        val phase = memory.getValue(null)
        // The runs of a dynamic phase are summed up, and the enclosing phase includes them
        assertTrue(dynamicPhase.allocatedBytes >= 2L * allocationSize, dynamicPhase.toString())
        assertTrue(dynamicPhase.gcCount >= 2, dynamicPhase.toString())
        assertTrue(dynamicPhase.peakHeapBytes >= allocationSize, dynamicPhase.toString())
        assertTrue(phase.allocatedBytes >= dynamicPhase.allocatedBytes, phase.toString())
        assertTrue(phase.gcCount >= dynamicPhase.gcCount, phase.toString())
    }

    @Test
    fun nativeMemoryIsReportedInTheResidentSetSize() {
        assumeTrue(FlightRecorder.getFlightRecorder().eventTypes.any { it.name == "jdk.ResidentSetSize" }, "Needs JDK 21")
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }
        perfManager.measureCompilation().use {
            perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
                // Like the memory of LLVM, a direct buffer is outside the heap
                val buffer = ByteBuffer.allocateDirect(allocationSize)
                for (i in 0 until allocationSize step 4096) buffer.put(i, 1)
                // Longer than the sampling period
                Thread.sleep(100)
            }
        }

        val memory = perfManager.unitStats.memoryStats!!.single { it.dynamicPhase == null }
        val peakRss = assertNotNull(memory.peakRssBytes, memory.toString())
        assertTrue(peakRss >= allocationSize, memory.toString())
    }

    @Test
    fun phasesOfChildManagersOnOtherThreadsAreReported() {
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }
        perfManager.measureCompilation().use {
            thread {
                val child = PerformanceManagerImpl.createChildIfNeeded(perfManager, start = false)!!
                child.tryMeasureDynamicPhaseTime("In child", PhaseType.Backend, ::allocateAndCollect)
            }.join()
        }

        val memory = perfManager.unitStats.memoryStats!!.single { it.dynamicPhase == "In child" }
        assertTrue(memory.allocatedBytes >= allocationSize, memory.toString())
    }

    @Test
    fun phasesOfOtherCompilationsAreNotReported() {
        // Like a Kotlin daemon running another compilation alongside this one
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }
        perfManager.measureCompilation().use {
            thread {
                val other = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Other")
                other.tryMeasureDynamicPhaseTime("In other compilation", PhaseType.Backend, ::allocateAndCollect)
            }.join()
            perfManager.tryMeasureDynamicPhaseTime("In this compilation", PhaseType.Backend, ::allocateAndCollect)
        }

        assertEquals(listOf("In this compilation"), perfManager.unitStats.memoryStats!!.mapNotNull { it.dynamicPhase })
    }

    private fun runningMemoryRecordings() =
        FlightRecorder.getFlightRecorder().recordings.count { it.name == "Kotlin compilation phases" }

    @Test
    fun theMeasurementIsFinishedByTheCodeThatStartedIt() {
        val before = runningMemoryRecordings()
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }

        perfManager.measureCompilation().use {
            // Like the first stage of a one-stage Kotlin/Native compilation
            perfManager.enableMemoryTracking()
            perfManager.measureCompilation().use {}
            assertFalse(perfManager.isFinalized)
            assertEquals(before + 1, runningMemoryRecordings())
        }

        assertTrue(perfManager.isFinalized)
        assertEquals(before, runningMemoryRecordings())
        assertNotNull(perfManager.unitStats.memoryStats)
    }

    @Test
    fun theMeasurementIsFinishedWhenTheCompilationFails() {
        val before = runningMemoryRecordings()
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test").apply { enableMemoryTracking() }

        assertFailsWith<IllegalStateException> {
            perfManager.measureCompilation().use { error("Compilation failed") }
        }

        assertTrue(perfManager.isFinalized)
        assertEquals(before, runningMemoryRecordings())
    }

    @Test
    fun memoryIsNotReportedWhenTrackingIsDisabled() {
        val before = runningMemoryRecordings()
        val perfManager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test")

        perfManager.measureCompilation().use {
            assertEquals(before, runningMemoryRecordings())
            perfManager.tryMeasureDynamicPhaseTime("Phase", PhaseType.Backend, ::allocateAndCollect)
        }

        assertNull(perfManager.unitStats.memoryStats)
    }
}
