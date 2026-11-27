/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import jdk.jfr.FlightRecorder
import jdk.jfr.Recording
import jdk.jfr.consumer.RecordedEvent
import jdk.jfr.consumer.RecordingFile
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * Records real Java Flight Recorder events and reads them back.
 *
 * Asserting on `commit()` being called would prove nothing: without a running recording that has the event
 * enabled, `commit()` is a silent no-op, so an implementation that emits nothing at all would pass just as well.
 */
class CompilationPhaseEventTest {
    private val mb = 1024 * 1024

    @Volatile
    private var sink: Any? = null

    private fun allocate(bytes: Int) {
        sink = ByteArray(bytes)
        sink = null
    }

    private fun createPerformanceManager() = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Test")

    /** Like a recording started with `-XX:StartFlightRecording`, this one is unrelated to the memory tracking of [PerformanceManager.enableMemoryTracking]. */
    private fun recordingPhaseEvents(block: () -> Unit): List<RecordedEvent> {
        val file = Files.createTempFile("kotlin-compilation-phase-events", ".jfr")
        val recording = Recording()
        try {
            recording.enable(EVENT_NAME).withoutThreshold()
            recording.start()
            block()
            recording.stop()
            recording.dump(file)
            return RecordingFile.readAllEvents(file).filter { it.eventType.name == EVENT_NAME }
        } finally {
            recording.close()
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun phasesAreRecordedWithoutMemoryTracking() {
        val perfManager = createPerformanceManager()

        val events = recordingPhaseEvents {
            perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
                perfManager.tryMeasureDynamicPhaseTime("Devirtualization", PhaseType.Backend) { allocate(16 * mb) }
            }
        }

        val dynamicPhase = events.single { it.getString("dynamicPhase") == "Devirtualization" }
        assertEquals("Backend", dynamicPhase.getString("phaseType"))
        assertTrue(dynamicPhase.getLong("allocated") >= 16 * mb, dynamicPhase.toString())
        assertTrue(dynamicPhase.getLong("heapUsedAtEnd") > 0, dynamicPhase.toString())

        val phase = events.single { it.getString("phaseType") == "Backend" && it !== dynamicPhase }
        assertNull(phase.getString("dynamicPhase"))
        assertTrue(phase.getLong("allocated") >= dynamicPhase.getLong("allocated"), phase.toString())
        // The phase shows up on the timeline as a span enclosing its dynamic phases
        assertTrue(!phase.startTime.isAfter(dynamicPhase.startTime) && !phase.endTime.isBefore(dynamicPhase.endTime))
    }

    @Test
    fun eachRunOfARepeatedlyMeasuredPhaseIsRecordedSeparately() {
        // The report merges the runs of a file lowering; the recording keeps them apart, so a phase that got
        // expensive on one particular file is still visible
        val perfManager = createPerformanceManager()
        val sizes = listOf(1 * mb, 4 * mb, 16 * mb)

        val events = recordingPhaseEvents {
            for (size in sizes) {
                perfManager.tryMeasureDynamicPhaseTime("File lowering", PhaseType.IrLowering) { allocate(size) }
            }
        }

        val runs = events.filter { it.getString("dynamicPhase") == "File lowering" }.sortedBy { it.startTime }
        assertEquals(sizes.size, runs.size, runs.toString())
        for (i in sizes.indices) {
            val allocated = runs[i].getLong("allocated")
            assertTrue(allocated >= sizes[i] && allocated < 2L * sizes[i], "Expected ~${sizes[i]} bytes: ${runs[i]}")
        }
    }

    @Test
    fun shortPhasesAreLeftOutOfRecordingsByDefault() {
        // Make sure the event is registered
        createPerformanceManager().tryMeasureDynamicPhaseTime("Phase", PhaseType.Backend) {}

        // Checked on the declaration rather than with a recording: events are shared by all the recordings in the process,
        // so one started for the test JVM with its own settings could lift the threshold and record the short phases anyway
        val eventType = FlightRecorder.getFlightRecorder().eventTypes.single { it.name == EVENT_NAME }
        assertEquals("1 ms", eventType.settingDescriptors.single { it.name == "threshold" }.defaultValue)
    }

    companion object {
        /** Tools read a recording by event name, so it is part of the contract with them and is spelled out here. */
        private const val EVENT_NAME = "org.jetbrains.kotlin.CompilationPhase"
    }
}
