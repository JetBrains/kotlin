/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import jdk.jfr.Category
import jdk.jfr.DataAmount
import jdk.jfr.Event
import jdk.jfr.EventType
import jdk.jfr.Label
import jdk.jfr.Name
import jdk.jfr.Recording
import jdk.jfr.StackTrace
import jdk.jfr.Threshold
import jdk.jfr.consumer.RecordedEvent
import jdk.jfr.consumer.RecordingFile
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * A phase measured by [PerformanceManager], as a Java Flight Recorder event.
 *
 * The events are emitted whenever a recording asks for them, so the phases are also visible in a recording started
 * with `-XX:StartFlightRecording` and can be correlated there with any other JFR event (allocation samples, GC, etc.).
 * The threshold keeps the numerous sub-millisecond per-file phases out of such recordings; [PhaseMemoryRecording] lifts it.
 *
 * A process like a Kotlin daemon may run several compilations at once, [compilationId] tells their phases apart.
 */
@Name(CompilationPhaseEvent.NAME)
@Category("Kotlin", "Compiler")
@Label("Compilation Phase")
@StackTrace(false)
@Threshold("1 ms")
internal class CompilationPhaseEvent : Event() {
    @Label("Compilation Id")
    var compilationId: Long = 0

    @Label("Phase Type")
    var phaseType: String = ""

    @Label("Dynamic Phase")
    var dynamicPhase: String? = null

    @Label("Heap Used At End")
    @DataAmount
    var heapUsedAtEnd: Long = 0

    @Label("Allocated By Thread")
    @DataAmount
    var allocated: Long = 0

    fun finish(compilationId: Long, phaseType: PhaseType, dynamicPhase: String?) {
        end()
        if (!shouldCommit()) return
        this.compilationId = compilationId
        this.phaseType = phaseType.name
        this.dynamicPhase = dynamicPhase
        heapUsedAtEnd = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        allocated = allocatedByCurrentThread() - allocated
        commit()
    }

    companion object {
        const val NAME: String = "org.jetbrains.kotlin.CompilationPhase"

        private val eventType = EventType.getEventType(CompilationPhaseEvent::class.java)
        private val threadMXBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

        // Not `currentThreadAllocatedBytes`, which needs JDK 14
        @Suppress("DEPRECATION")
        private fun allocatedByCurrentThread(): Long = threadMXBean.getThreadAllocatedBytes(Thread.currentThread().id)

        /** Returns `null` when no recording wants the event, which keeps the measurements free in that case. */
        fun startIfEnabled(): CompilationPhaseEvent? {
            if (!eventType.isEnabled) return null
            return CompilationPhaseEvent().apply {
                allocated = allocatedByCurrentThread()
                begin()
            }
        }
    }
}

/**
 * An in-process JFR recording of the [CompilationPhaseEvent]s and of the GC events that tell how much memory each phase used.
 *
 * The GC events are attributed to the phases by time after the recording is stopped, see [MemoryStats] for what that means.
 * The recording sees the events of the whole process, so only those of the compilation [compilationId] are kept,
 * which covers the child managers of the compilation whatever thread they run on.
 */
internal class PhaseMemoryRecording(private val compilationId: Long) {
    private val recording = Recording().apply {
        name = "Kotlin compilation phases"
        enable(CompilationPhaseEvent::class.java).withoutThreshold()
        enable(GC_HEAP_SUMMARY)
        enable(GARBAGE_COLLECTION)
        start()
    }

    fun stop(): List<MemoryStats> {
        val file = Files.createTempFile("kotlin-compilation-phases", ".jfr")
        val events = try {
            recording.stop()
            recording.dump(file)
            RecordingFile.readAllEvents(file)
        } finally {
            recording.close()
            Files.deleteIfExists(file)
        }

        // Between collections the heap only grows, so its peak during a phase is either the usage right before one of
        // the collections that happened during the phase or the usage at the end of the phase
        val heapUsedBeforeGc = TreeMap<Instant, Long>()
        val gcPauses = TreeMap<Instant, Duration>()
        for (event in events) {
            when (event.eventType.name) {
                GC_HEAP_SUMMARY -> if (event.getString("when") == "Before GC") heapUsedBeforeGc[event.startTime] = event.getLong("heapUsed")
                GARBAGE_COLLECTION -> gcPauses[event.startTime] = event.getDuration("sumOfPauses")
            }
        }

        fun <T> TreeMap<Instant, T>.during(event: RecordedEvent) = subMap(event.startTime, true, event.endTime, true).values

        return events.filter { it.eventType.name == CompilationPhaseEvent.NAME && it.getLong("compilationId") == compilationId }
            .map { event ->
                val pauses = gcPauses.during(event)
                MemoryStats(
                    PhaseType.valueOf(event.getString("phaseType")),
                    event.getString("dynamicPhase"),
                    peakHeapBytes = maxOf(event.getLong("heapUsedAtEnd"), heapUsedBeforeGc.during(event).maxOrNull() ?: 0),
                    allocatedBytes = event.getLong("allocated"),
                    gcCount = pauses.size,
                    gcPauseNanos = pauses.sumOf { it.toNanos() },
                )
            }
            .groupBy { it.phaseType to it.dynamicPhase }
            .values.map { it.reduce(MemoryStats::plus) }
    }

    private companion object {
        const val GC_HEAP_SUMMARY = "jdk.GCHeapSummary"
        const val GARBAGE_COLLECTION = "jdk.GarbageCollection"
    }
}
