/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import jdk.jfr.Category
import jdk.jfr.DataAmount
import jdk.jfr.Event
import jdk.jfr.EventType
import jdk.jfr.FlightRecorder
import jdk.jfr.Label
import jdk.jfr.Name
import jdk.jfr.Recording
import jdk.jfr.StackTrace
import jdk.jfr.Threshold
import jdk.jfr.consumer.RecordedEvent
import jdk.jfr.consumer.RecordingFile
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * A phase measured by [PerformanceManager], as a Java Flight Recorder event.
 *
 * The events are emitted whenever a recording asks for them, so the phases are also visible in a recording started
 * with `-XX:StartFlightRecording` and can be correlated there with any other JFR event (allocation samples, GC, etc.).
 * The threshold keeps the numerous sub-millisecond per-file phases out of such recordings. [PhaseMemoryRecording] lifts it,
 * and as the flight recorder applies the lowest threshold of all running recordings, such recordings get all phases meanwhile.
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
 * Whether Java Flight Recorder can be used. `jdk.jfr` is missing from some runtimes, like a jlinked one or an old JDK 8,
 * and [CompilationPhaseEvent] fails to load there, so nothing may touch it unless this is `true`.
 */
internal object FlightRecorderSupport {
    val isAvailable: Boolean by lazy {
        try {
            // Initializing the event checks all it needs: `jdk.jfr` and the thread allocation counter
            Class.forName(CompilationPhaseEvent::class.java.name, true, CompilationPhaseEvent::class.java.classLoader)
            FlightRecorder.isAvailable()
        } catch (_: LinkageError) {
            false
        }
    }
}

/**
 * An in-process JFR recording of the [CompilationPhaseEvent]s and of the GC and resident set size events that tell
 * how much memory each phase used.
 *
 * The GC and resident set size events are attributed to the phases by time after the recording is stopped,
 * see [MemoryStats] for what that means.
 * The recording sees the events of the whole process, so only those of the compilation [compilationId] are kept,
 * which covers the child managers of the compilation whatever thread they run on.
 */
internal class PhaseMemoryRecording(private val compilationId: Long) {
    private val recording = Recording().apply {
        name = "Kotlin compilation phases"
        enable(CompilationPhaseEvent::class.java).withoutThreshold()
        enable(GC_HEAP_SUMMARY)
        enable(GARBAGE_COLLECTION)
        // Ignored before JDK 21, which doesn't have the event
        enable(RESIDENT_SET_SIZE).withPeriod(RSS_SAMPLING_PERIOD)
        start()
    }

    fun stop(): List<MemoryStats> {
        var file: Path? = null
        try {
            recording.stop()
            file = Files.createTempFile("kotlin-compilation-phases", ".jfr")
            recording.dump(file)

            // Between collections the heap only grows, so its peak during a phase is either the usage right before one of
            // the collections that happened during the phase or the usage at the end of the phase
            val heapUsedBeforeGc = TreeMap<Instant, Long>()
            val gcPauses = TreeMap<Instant, Duration>()
            val rssSamples = TreeMap<Instant, RssSample>()
            // The events are read twice rather than kept in memory: per-file phases of a large module produce a lot of them
            file.forEachEvent { event ->
                when (event.eventType.name) {
                    GC_HEAP_SUMMARY -> if (event.getString("when") == "Before GC") heapUsedBeforeGc[event.startTime] = event.getLong("heapUsed")
                    GARBAGE_COLLECTION -> gcPauses[event.startTime] = event.getDuration("sumOfPauses")
                    RESIDENT_SET_SIZE -> rssSamples[event.startTime] =
                        RssSample(event.getLong("size"), if (event.hasField("peak")) event.getLong("peak") else 0)
                }
            }

            fun <T> TreeMap<Instant, T>.during(event: RecordedEvent) = subMap(event.startTime, true, event.endTime, true).values

            // The peak of the process only grows, so if it grew between the samples around the start and the end of a phase,
            // it was reached about during the phase, maybe between two samples
            fun peakRss(event: RecordedEvent): Long? {
                val samples = rssSamples.during(event).ifEmpty { return null }
                val peakBefore = rssSamples.lowerEntry(event.startTime)?.value?.peak
                val newPeak = samples.last().peak.takeIf { peakBefore != null && it > peakBefore } ?: 0
                return maxOf(samples.maxOf { it.size }, newPeak)
            }

            val memoryStats = LinkedHashMap<Pair<PhaseType, String?>, MemoryStats>()
            file.forEachEvent { event ->
                if (event.eventType.name != CompilationPhaseEvent.NAME || event.getLong("compilationId") != compilationId) return@forEachEvent
                val pauses = gcPauses.during(event)
                val stats = MemoryStats(
                    PhaseType.valueOf(event.getString("phaseType")),
                    event.getString("dynamicPhase"),
                    peakHeapBytes = maxOf(event.getLong("heapUsedAtEnd"), heapUsedBeforeGc.during(event).maxOrNull() ?: 0),
                    allocatedBytes = event.getLong("allocated"),
                    gcCount = pauses.size,
                    gcPauseNanos = pauses.sumOf { it.toNanos() },
                    peakRssBytes = peakRss(event),
                )
                memoryStats.merge(stats.phaseType to stats.dynamicPhase, stats, MemoryStats::plus)
            }
            return memoryStats.values.toList()
        } finally {
            recording.close()
            file?.let(Files::deleteIfExists)
        }
    }

    private class RssSample(val size: Long, val peak: Long)

    private fun Path.forEachEvent(action: (RecordedEvent) -> Unit) {
        RecordingFile(this).use { while (it.hasMoreEvents()) action(it.readEvent()) }
    }

    private companion object {
        const val GC_HEAP_SUMMARY = "jdk.GCHeapSummary"
        const val GARBAGE_COLLECTION = "jdk.GarbageCollection"
        const val RESIDENT_SET_SIZE = "jdk.ResidentSetSize"
        val RSS_SAMPLING_PERIOD: Duration = Duration.ofMillis(10)
    }
}
