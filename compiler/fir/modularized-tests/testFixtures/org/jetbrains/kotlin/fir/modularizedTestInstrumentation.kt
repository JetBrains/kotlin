/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.SideStats
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/*
 * Measurement harness for the generated isolated full pipeline tests (e.g. IntelliJFullPipelineTestsGenerated).
 *
 * Every generated test compiles a single module in its own thread, so each test is a natural measurement unit.
 * For every such compilation a JSON record is appended to `compilations.jsonl` in the run directory, and the
 * environment description together with the calibration probes is stored in `manifest.json` / `summary.json`.
 *
 * The primary purpose is comparing machines and system configurations (e.g. with and without endpoint security
 * agents that hook file system syscalls). Such agents are mostly invisible in their own CPU consumption: the cost
 * is paid as extra system time and extra latency inside the measured process. Therefore the records keep
 * wall/user/cpu times separately (system time = cpu - user) and store the number and the total time of class
 * lookups, which is the most syscall-intensive part of the compilation.
 *
 * System properties (all optional):
 *   fir.bench.instrumentation           - `false` disables the harness completely (default `true`)
 *   fir.bench.instrumentation.dir       - root directory for run directories (default `tmp/fir-bench`)
 *   fir.bench.instrumentation.label     - human-readable label of the configuration, e.g. `m5max-falcon-on`
 *   fir.bench.instrumentation.detailed  - `false` disables per-thread user/cpu time measurement (default `true`)
 *   fir.bench.instrumentation.probes    - `false` disables the environment probes (default `true`)
 *   fir.bench.instrumentation.probes.suite - `full` (default), `quick` (fewer repetitions, smaller working sets)
 *                                         or `off` (the same as `probes=false`)
 *   fir.bench.instrumentation.probes.<name>.iterations - repetitions of an individual probe, e.g.
 *                                         `fir.bench.instrumentation.probes.processExec.iterations=100`
 *   fir.bench.sample.fraction           - compile only a deterministic subset of the modules (default `1.0`).
 *                                         The subset is selected by a stable hash of the model file name, so it is
 *                                         the same on every machine and in every run with the same seed
 *   fir.bench.sample.seed               - changes which subset is selected (default `0`)
 *   fir.bench.sample.list               - path to a file with the model file names to compile, one per line;
 *                                         overrides the fraction
 *   fir.bench.compile.repeat            - compile every selected module this many times in a row (default `1`);
 *                                         every compilation gets its own record with an `iteration` field
 */
object ModularizedTestInstrumentation {
    val enabled: Boolean = System.getProperty("fir.bench.instrumentation", "true").toBooleanLenient()

    /** Enables per-thread user/cpu time measurements inside the compiler, otherwise only wall time is available. */
    val detailedPerf: Boolean = System.getProperty("fir.bench.instrumentation.detailed", "true").toBooleanLenient()

    private val probeSuite: String = System.getProperty("fir.bench.instrumentation.probes.suite", "full").lowercase(Locale.ENGLISH)

    private val probesEnabled: Boolean =
        System.getProperty("fir.bench.instrumentation.probes", "true").toBooleanLenient() && probeSuite != "off"

    private val quickProbes: Boolean = probeSuite == "quick"

    /**
     * A deterministic subset of the modules: the selection is a stable hash of the model file name, so every
     * machine and every repetition compiles exactly the same modules. Needed because the full suite at
     * `parallelism=1` takes about an hour, while a comparison needs several alternating repetitions.
     */
    private val sampleFraction: Double = System.getProperty("fir.bench.sample.fraction")?.toDoubleOrNull() ?: 1.0

    private val sampleSeed: Int = System.getProperty("fir.bench.sample.seed")?.toIntOrNull() ?: 0

    private val sampleList: Set<String>? = System.getProperty("fir.bench.sample.list")?.let { path ->
        try {
            File(path).readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        } catch (e: Exception) {
            System.err.println("Can't read the module sample list $path: ${e.message}")
            null
        }
    }

    /**
     * Compiling the same module several times in a row separates the cold state (file cache, JIT, the class path
     * caches of the compiler) from the steady state: the first iteration is the cold one, the rest are warm.
     */
    val compileRepeat: Int = Math.max(1, System.getProperty("fir.bench.compile.repeat")?.toIntOrNull() ?: 1)

    private val skippedBySampling = AtomicInteger()

    private val label: String = System.getProperty("fir.bench.instrumentation.label") ?: "default"

    private val rootDir: File = File(System.getProperty("fir.bench.instrumentation.dir") ?: "tmp/fir-bench")

    private val runStartMs: Long = System.currentTimeMillis()
    private val runStartNanos: Long = System.nanoTime()

    private val compilationsCount = AtomicInteger()
    private val failedCompilationsCount = AtomicInteger()
    private val totalCompilationWallNanos = AtomicLong()

    /**
     * The number of the compilations running at the same moment. Two runs are comparable only if this number is the
     * same, otherwise the difference in the heap pressure and in the scheduling dominates any effect being measured.
     */
    private val inFlightCompilations = AtomicInteger()

    private val threadMXBean = ManagementFactory.getThreadMXBean().also {
        if (it.isThreadCpuTimeSupported) it.isThreadCpuTimeEnabled = true
    }

    val runDir: File by lazy {
        val stamp = SimpleDateFormat("yyyy-MM-dd__HH-mm-ss", Locale.ENGLISH).format(Date(runStartMs))
        File(rootDir, "$stamp-${label.sanitized()}").also {
            it.mkdirs()
            writeManifest(it)
            Runtime.getRuntime().addShutdownHook(Thread { writeSummary(it) })
        }
    }

    private val recordsFile: File by lazy { File(runDir, "compilations.jsonl") }

    fun createPerformanceManager(): PerformanceManagerImpl? {
        if (!enabled) return null
        return PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Modularized isolated test performance manager").also {
            it.detailedPerf = detailedPerf
        }
    }

    /**
     * `false` means the module is excluded from this run by the `fir.bench.sample.*` configuration and the test
     * must be skipped (not failed).
     */
    fun isSelected(modelPath: String): Boolean {
        if (!enabled) return true
        val name = File(modelPath).name
        sampleList?.let { return name in it }
        if (sampleFraction >= 1.0) return true
        if (sampleFraction <= 0.0) return false
        // FNV-1a over the file name: stable across the JVM versions and the machines, unlike String.hashCode
        var hash = 2166136261L xor (sampleSeed.toLong() and 0xffffffffL)
        for (char in name) {
            hash = (hash xor char.code.toLong()) * 16777619L and 0xffffffffL
        }
        return hash % 10_000L < sampleFraction * 10_000L
    }

    fun noteSkippedBySampling(): Int = skippedBySampling.incrementAndGet()

    fun start(modelPath: String, iteration: Int = 0): CompilationMeasurement? {
        if (!enabled) return null
        // Create the run directory (and run the initial probes) before the first compilation starts
        runDir
        return CompilationMeasurement(
            modelPath = modelPath,
            threadName = Thread.currentThread().name,
            startTimeMs = System.currentTimeMillis(),
            startWallNanos = System.nanoTime(),
            startThreadCpuNanos = currentThreadCpuNanos(),
            startThreadUserNanos = currentThreadUserNanos(),
            startProcessCpuNanos = processCpuNanos(),
            inFlightAtStart = inFlightCompilations.incrementAndGet(),
            startGcMillis = totalGcMillis(),
            startGcCount = totalGcCount(),
            startJitMillis = totalJitMillis(),
            iteration = iteration,
        )
    }

    fun finish(measurement: CompilationMeasurement?, performanceManager: PerformanceManager?, result: ExitCode) {
        if (!enabled || measurement == null) return

        val stats = try {
            performanceManager?.unitStats
        } catch (e: Throwable) {
            System.err.println("Can't collect the compiler measurements: ${e.message}")
            null
        }

        val wallNanos = System.nanoTime() - measurement.startWallNanos
        val threadCpuNanos = currentThreadCpuNanos() - measurement.startThreadCpuNanos
        val threadUserNanos = currentThreadUserNanos() - measurement.startThreadUserNanos
        val processCpuNanos = processCpuNanos() - measurement.startProcessCpuNanos
        val inFlightAtEnd = inFlightCompilations.getAndDecrement()

        compilationsCount.incrementAndGet()
        if (result != ExitCode.OK) failedCompilationsCount.incrementAndGet()
        totalCompilationWallNanos.addAndGet(wallNanos)

        val json = json {
            field("model", File(measurement.modelPath).name)
            field("modelPath", measurement.modelPath)
            field("module", stats?.name)
            field("exitCode", result.name)
            field("thread", measurement.threadName)
            field("startTimeMs", measurement.startTimeMs)
            // 0 is the cold compilation of this module, the following ones are warm; see `fir.bench.compile.repeat`
            field("iteration", measurement.iteration)
            field("repeatCount", compileRepeat)
            field("wallNanos", wallNanos)
            // The test thread itself; the compiler may use additional threads, see `processCpuNanos`
            field("threadCpuNanos", threadCpuNanos)
            field("threadUserNanos", threadUserNanos)
            field("threadSystemNanos", threadCpuNanos - threadUserNanos)
            // Whole JVM, so it is polluted by the concurrently running tests, useful only in the sequential mode
            field("processCpuNanos", processCpuNanos)
            // How many compilations were running in parallel; the runs being compared must agree on these numbers
            field("inFlightAtStart", measurement.inFlightAtStart)
            field("inFlightAtEnd", inFlightAtEnd)
            // Deltas over this compilation. Whole JVM as well, but unlike an absolute snapshot they at least
            // show where the time of a suspiciously slow compilation went
            field("gcDeltaMillis", totalGcMillis() - measurement.startGcMillis)
            field("gcDeltaCount", totalGcCount() - measurement.startGcCount)
            field("jitDeltaMillis", totalJitMillis() - measurement.startJitMillis)
            if (stats != null) {
                field("files", stats.filesCount)
                field("lines", stats.linesCount)
                field("measuredCpuAndUserTime", stats.measuredCpuAndUserTime)
                objectField("phases") {
                    stats.forEachPhaseMeasurement { phaseType, time ->
                        if (time != null) objectField(phaseType.name) { time(time) }
                    }
                    objectField("total") { time(stats.getTotalTime()) }
                }
                // The number and the total time of class file lookups: the most syscall-intensive part of the pipeline
                objectField("findJavaClass") { sideStats(stats.findJavaClassStats) }
                objectField("findKotlinClass") { sideStats(stats.findKotlinClassStats) }
                arrayField("gc") {
                    for (gc in stats.gcStats) {
                        item { field("kind", gc.kind); field("millis", gc.millis); field("count", gc.count) }
                    }
                }
                field("jitTimeMillis", stats.jitTimeMillis)
            }
        }

        synchronized(this) {
            FileOutputStream(recordsFile, true).use { output ->
                OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.write("\n")
                }
            }
        }
    }

    private fun writeManifest(dir: File) {
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val text = json {
            field("label", label)
            field("startTimeMs", runStartMs)
            field("startTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date(runStartMs)))
            field("host", commandOutput("hostname"))
            field("osName", System.getProperty("os.name"))
            field("osVersion", System.getProperty("os.version"))
            field("osArch", System.getProperty("os.arch"))
            field("availableProcessors", Runtime.getRuntime().availableProcessors())
            field("maxHeapBytes", Runtime.getRuntime().maxMemory())
            field("javaVersion", System.getProperty("java.version"))
            field("javaVendor", System.getProperty("java.vendor"))
            field("javaHome", System.getProperty("java.home"))
            arrayField("jvmArgs") { for (arg in runtimeMXBean.inputArguments) item(arg) }
            // The parallelism of the test engine and of the harness itself: the most important thing to pin
            objectField("testConfiguration") {
                for (name in COMPARED_SYSTEM_PROPERTIES) field(name, System.getProperty(name))
                for (name in System.getProperties().stringPropertyNames().sorted()) {
                    if (name.startsWith("fir.bench.")) field(name, System.getProperty(name))
                }
                // The effective values, so that a comparison does not depend on which properties were set explicitly
                field("sampleFraction", sampleFraction)
                field("sampleSeed", sampleSeed)
                field("sampleListSize", sampleList?.size ?: 0)
                field("compileRepeat", compileRepeat)
                field("probeSuite", probeSuite)
                field("maxHeapBytesEffective", Runtime.getRuntime().maxMemory())
            }
            objectField("system") { systemDescription() }
            if (probesEnabled) {
                objectField("probesBefore") { probes(dir) }
            }
        }
        File(dir, "manifest.json").writeTextSafely(text)
    }

    private fun writeSummary(dir: File) {
        // Measure the run before settling, otherwise the settling pause would be counted into it
        val runWallNanos = System.nanoTime() - runStartNanos
        if (probesEnabled) settleBeforeProbes()
        val text = json {
            field("label", label)
            field("compilations", compilationsCount.get())
            field("failedCompilations", failedCompilationsCount.get())
            field("skippedBySampling", skippedBySampling.get())
            field("runWallNanos", runWallNanos)
            field("sumOfCompilationWallNanos", totalCompilationWallNanos.get())
            field("processCpuNanos", processCpuNanos())
            arrayField("gc") {
                for (gc in ManagementFactory.getGarbageCollectorMXBeans()) {
                    item { field("kind", gc.name); field("millis", gc.collectionTime); field("count", gc.collectionCount) }
                }
            }
            if (probesEnabled) {
                objectField("probesAfter") { probes(dir) }
            }
        }
        File(dir, "summary.json").writeTextSafely(text)
    }

    /**
     * Describes the state of the machine that may affect the measurements, in particular the endpoint security
     * agents that hook the file system syscalls of every process.
     */
    private fun JsonBuilder.systemDescription() {
        if (System.getProperty("os.name").orEmpty().startsWith("Mac")) {
            field("cpuBrand", commandOutput("sysctl", "-n", "machdep.cpu.brand_string"))
            field("physicalMemoryBytes", commandOutput("sysctl", "-n", "hw.memsize"))
            field("swVers", commandOutput("sw_vers", "-productVersion"))
            field("systemExtensions", commandOutput("systemextensionsctl", "list"))
            field("securityAgentProcesses", commandOutput("pgrep", "-fl", "falcon|kandji|littlesnitch"))
        }
    }

    /**
     * The calibration suite, see [CalibrationProbes]. Two different machines cannot be compared without it: a
     * faster CPU, a faster memory subsystem and a machine without a monitoring agent all look the same in the
     * workload numbers alone. One probe per dimension, so that the analysis can build a predicted ratio out of
     * them and report what is left unexplained (`scripts/calibrate.py`, `scripts/compare-bench-runs.py`).
     */
    private fun JsonBuilder.probes(dir: File) {
        val probeDir = File(dir, "probe-${probeDirCounter.incrementAndGet()}").also { it.mkdirs() }
        val results = try {
            CalibrationProbes.run(probeDir, quickProbes) { name ->
                System.getProperty("fir.bench.instrumentation.probes.$name.iterations")?.toIntOrNull()
            }
        } catch (e: Throwable) {
            System.err.println("The calibration probes failed: ${e.message}")
            emptyList()
        } finally {
            probeDir.deleteRecursively()
        }
        for (probe in results) {
            for (name in listOf(probe.name) + probe.aliases) {
                objectField(name) { measurements(probe) }
            }
        }
    }

    private val probeDirCounter = AtomicInteger()

    /**
     * The `probesAfter` pass would otherwise start right after the last compilation, on a heap full of garbage and
     * with the JIT and GC threads still working, which made it noticeably noisier than `probesBefore` (a spread of
     * 60% on the memory latency probes against 5% before). A calibration factor is worthless if the two passes of
     * the same run disagree more than the two machines do.
     */
    private fun settleBeforeProbes() {
        System.gc()
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(3))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun JsonBuilder.measurements(probe: CalibrationProbes.ProbeResult) {
        val sorted = probe.nanos.sortedArray()
        val median = sorted.percentile(0.5)
        field("kind", probe.kind)
        field("unit", probe.unit)
        field("opsPerMeasurement", probe.opsPerMeasurement)
        field("bytesPerMeasurement", probe.bytesPerMeasurement)
        field("lowerIsBetter", probe.unit != CalibrationProbes.UNIT_BYTES_PER_SECOND)
        field("count", sorted.size)
        field("totalNanos", sorted.sum())
        field("meanNanos", if (sorted.isEmpty()) 0 else sorted.sum() / sorted.size)
        field("minNanos", sorted.firstOrNull() ?: 0)
        field("medianNanos", median)
        field("p95Nanos", sorted.percentile(0.95))
        field("maxNanos", sorted.lastOrNull() ?: 0)
        // The derived per-operation figures, so that no consumer has to know the shape of a particular probe
        if (probe.opsPerMeasurement > 0) {
            field("medianNanosPerOp", median.toDouble() / probe.opsPerMeasurement)
        }
        if (probe.bytesPerMeasurement > 0 && median > 0) {
            field("medianBytesPerSecond", probe.bytesPerMeasurement * 1_000_000_000L / median)
        }
    }

    private fun LongArray.percentile(ratio: Double): Long {
        if (isEmpty()) return 0
        val index = Math.min(size - 1, Math.max(0, Math.round(ratio * (size - 1)).toInt()))
        return this[index]
    }

    private fun JsonBuilder.time(time: Time) {
        field("nanos", time.nanos)
        field("userNanos", time.userNanos)
        field("cpuNanos", time.cpuNanos)
        field("systemNanos", time.cpuNanos - time.userNanos)
    }

    private fun JsonBuilder.sideStats(sideStats: SideStats?) {
        val stats = sideStats ?: SideStats.EMPTY
        field("count", stats.count)
        time(stats.time)
        field("nanosPerCall", if (stats.count == 0) 0 else stats.time.nanos / stats.count)
    }

    private fun currentThreadCpuNanos(): Long = if (threadMXBean.isThreadCpuTimeSupported) threadMXBean.currentThreadCpuTime else -1

    private fun currentThreadUserNanos(): Long = if (threadMXBean.isThreadCpuTimeSupported) threadMXBean.currentThreadUserTime else -1

    private val processCpuTimeMethod = try {
        Class.forName("com.sun.management.OperatingSystemMXBean").getMethod("getProcessCpuTime")
    } catch (e: Throwable) {
        null
    }

    private fun processCpuNanos(): Long = try {
        processCpuTimeMethod?.invoke(ManagementFactory.getOperatingSystemMXBean()) as? Long ?: -1L
    } catch (e: Throwable) {
        -1L
    }

    private fun totalGcMillis(): Long {
        var result = 0L
        for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.collectionTime > 0) result += bean.collectionTime
        }
        return result
    }

    private fun totalGcCount(): Long {
        var result = 0L
        for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.collectionCount > 0) result += bean.collectionCount
        }
        return result
    }

    private fun totalJitMillis(): Long = ManagementFactory.getCompilationMXBean()?.totalCompilationTime ?: 0L

    private fun commandOutput(vararg command: String): String? = try {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.reader(Charsets.UTF_8).readText().trim()
        process.waitFor(10, TimeUnit.SECONDS)
        output.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    private fun File.writeTextSafely(text: String) {
        try {
            writeText(text)
        } catch (e: Exception) {
            System.err.println("Can't write the instrumentation report to $this: ${e.message}")
        }
    }

    private fun String.sanitized(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun String.toBooleanLenient(): Boolean = equals("true", ignoreCase = true) || this == "1" || equals("yes", ignoreCase = true)

    /** The properties that must be identical in the runs being compared, see `scripts/run-bench.sh`. */
    private val COMPARED_SYSTEM_PROPERTIES = listOf(
        "junit.jupiter.execution.parallel.enabled",
        "junit.jupiter.execution.parallel.config.strategy",
        "junit.jupiter.execution.parallel.config.fixed.parallelism",
        "junit.jupiter.execution.parallel.config.dynamic.factor",
    )
}

class CompilationMeasurement internal constructor(
    val modelPath: String,
    val threadName: String,
    val startTimeMs: Long,
    val startWallNanos: Long,
    val startThreadCpuNanos: Long,
    val startThreadUserNanos: Long,
    val startProcessCpuNanos: Long,
    val inFlightAtStart: Int,
    val startGcMillis: Long,
    val startGcCount: Long,
    val startJitMillis: Long,
    val iteration: Int,
)

/*
 * A minimalistic JSON writer: the module has no JSON library on the test fixtures classpath, and the records are simple.
 */
private class JsonBuilder(private val builder: StringBuilder, private val indent: String) {
    private var hasItems = false

    fun field(name: String, value: String?) {
        if (value == null) return
        appendSeparator()
        builder.append(indent).append(name.quoted()).append(": ").append(value.quoted())
    }

    fun field(name: String, value: Number?) {
        if (value == null) return
        appendSeparator()
        builder.append(indent).append(name.quoted()).append(": ").append(value)
    }

    fun field(name: String, value: Boolean) {
        appendSeparator()
        builder.append(indent).append(name.quoted()).append(": ").append(value)
    }

    fun objectField(name: String, body: JsonBuilder.() -> Unit) {
        appendSeparator()
        builder.append(indent).append(name.quoted()).append(": ")
        appendObject(body)
    }

    fun arrayField(name: String, body: JsonBuilder.() -> Unit) {
        appendSeparator()
        builder.append(indent).append(name.quoted()).append(": [")
        val nested = JsonBuilder(builder, "$indent  ")
        nested.body()
        if (nested.hasItems) builder.append("\n").append(indent)
        builder.append("]")
    }

    fun item(value: String) {
        appendSeparator()
        builder.append(indent).append(value.quoted())
    }

    fun item(body: JsonBuilder.() -> Unit) {
        appendSeparator()
        builder.append(indent)
        appendObject(body)
    }

    private fun appendObject(body: JsonBuilder.() -> Unit) {
        builder.append("{")
        val nested = JsonBuilder(builder, "$indent  ")
        nested.body()
        if (nested.hasItems) builder.append("\n").append(indent)
        builder.append("}")
    }

    private fun appendSeparator() {
        if (hasItems) builder.append(",")
        builder.append("\n")
        hasItems = true
    }

    private fun String.quoted(): String {
        val result = StringBuilder("\"")
        for (char in this) {
            when (char) {
                '"' -> result.append("\\\"")
                '\\' -> result.append("\\\\")
                '\n' -> result.append("\\n")
                '\r' -> result.append("\\r")
                '\t' -> result.append("\\t")
                else -> if (char < ' ') result.append("\\u%04x".format(char.code)) else result.append(char)
            }
        }
        return result.append("\"").toString()
    }
}

private fun json(body: JsonBuilder.() -> Unit): String {
    val builder = StringBuilder("{")
    val root = JsonBuilder(builder, "  ")
    root.body()
    return builder.append("\n}").toString()
}
