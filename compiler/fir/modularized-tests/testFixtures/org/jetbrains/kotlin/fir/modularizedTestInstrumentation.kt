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
import java.io.RandomAccessFile
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
 *   fir.bench.instrumentation.probes.cpu.iterations  - number of the cpu probe repetitions (default `30`)
 *   fir.bench.instrumentation.probes.exec.iterations - number of the process exec probe repetitions (default `30`)
 */
object ModularizedTestInstrumentation {
    val enabled: Boolean = System.getProperty("fir.bench.instrumentation", "true").toBooleanLenient()

    /** Enables per-thread user/cpu time measurements inside the compiler, otherwise only wall time is available. */
    val detailedPerf: Boolean = System.getProperty("fir.bench.instrumentation.detailed", "true").toBooleanLenient()

    private val probesEnabled: Boolean = System.getProperty("fir.bench.instrumentation.probes", "true").toBooleanLenient()

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

    fun start(modelPath: String): CompilationMeasurement? {
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
            }
            objectField("system") { systemDescription() }
            if (probesEnabled) {
                objectField("probesBefore") { probes(dir) }
            }
        }
        File(dir, "manifest.json").writeTextSafely(text)
    }

    private fun writeSummary(dir: File) {
        val text = json {
            field("label", label)
            field("compilations", compilationsCount.get())
            field("failedCompilations", failedCompilationsCount.get())
            field("runWallNanos", System.nanoTime() - runStartNanos)
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
     * Calibration probes. The CPU probe is required to compare different machines: all the other numbers should be
     * normalized by it, otherwise a faster CPU is indistinguishable from a machine without a monitoring agent.
     * The syscall probes measure exactly the operations intercepted by such agents.
     */
    private fun JsonBuilder.probes(dir: File) {
        objectField("cpu") { measurements(cpuProbe()) }
        val probeDir = File(dir, "probe-${probeDirCounter.incrementAndGet()}").also { it.mkdirs() }
        try {
            val files = createProbeFiles(probeDir)
            objectField("fileOpenRead") { measurements(fileOpenReadProbe(files)) }
            objectField("fileStat") { measurements(fileStatProbe(files)) }
            objectField("fileOpenReadUnique") { measurements(fileOpenReadUniqueProbe(probeDir)) }
            objectField("fileCreateDelete") { measurements(fileCreateDeleteProbe(probeDir)) }
            objectField("processExec") { measurements(processExecProbe()) }
        } finally {
            probeDir.deleteRecursively()
        }
    }

    private val probeDirCounter = AtomicInteger()

    private const val FILE_PROBE_COUNT = 512

    private val execProbeCount: Int = System.getProperty("fir.bench.instrumentation.probes.exec.iterations")?.toIntOrNull() ?: 30

    private val cpuProbeCount: Int = System.getProperty("fir.bench.instrumentation.probes.cpu.iterations")?.toIntOrNull() ?: 30

    private fun cpuProbe(): LongArray {
        val result = LongArray(cpuProbeCount)
        for (i in result.indices) {
            val start = System.nanoTime()
            var acc = 0L
            var x = 1L
            for (j in 0 until 20_000_000L) {
                x = x * 6364136223846793005L + 1442695040888963407L
                acc += x ushr 33
            }
            blackHole += acc
            result[i] = System.nanoTime() - start
        }
        return result
    }

    @Volatile
    private var blackHole: Long = 0

    private fun createProbeFiles(dir: File): List<File> = (0 until FILE_PROBE_COUNT).map { index ->
        File(dir, "probe-$index.bin").also { it.writeBytes(ByteArray(4096) { index.toByte() }) }
    }

    private fun fileOpenReadProbe(files: List<File>): LongArray = files.measureEach { file ->
        RandomAccessFile(file, "r").use { it.read(ByteArray(4096)) }
    }

    private fun fileStatProbe(files: List<File>): LongArray = files.measureEach { file ->
        file.length()
        file.canRead()
    }

    /**
     * Unlike [fileOpenReadProbe], every path here is opened exactly once, and the paths are spread over a deep
     * directory tree. A monitoring agent that caches its verdict per file cannot amortize such a probe, so this is
     * the probe that is expected to expose a synchronously hooked `open`.
     */
    private fun fileOpenReadUniqueProbe(dir: File): LongArray {
        val root = File(dir, "unique")
        val files = ArrayList<File>(FILE_PROBE_COUNT)
        val leafCount = 64
        val filesPerLeaf = FILE_PROBE_COUNT / leafCount
        for (leaf in 0 until leafCount) {
            val leafDir = File(root, "${leaf / 8}/${leaf % 8}").also { it.mkdirs() }
            for (index in 0 until filesPerLeaf) {
                files += File(leafDir, "unique-$leaf-$index.bin").also { it.writeBytes(ByteArray(4096)) }
            }
        }
        return files.measureEach { file -> RandomAccessFile(file, "r").use { it.read(ByteArray(4096)) } }
    }

    private fun fileCreateDeleteProbe(dir: File): LongArray = (0 until FILE_PROBE_COUNT).toList().measureEach { index ->
        val file = File(dir, "created-$index.bin")
        file.writeBytes(ByteArray(64))
        file.delete()
    }

    private fun processExecProbe(): LongArray = (0 until execProbeCount).toList().measureEach {
        try {
            ProcessBuilder("/usr/bin/true").start().waitFor()
        } catch (e: Exception) {
            // The probe is best effort only
        }
    }

    private inline fun <T> List<T>.measureEach(action: (T) -> Unit): LongArray {
        val result = LongArray(size)
        for (index in indices) {
            val start = System.nanoTime()
            action(this[index])
            result[index] = System.nanoTime() - start
        }
        return result
    }

    private fun JsonBuilder.measurements(nanos: LongArray) {
        val sorted = nanos.sortedArray()
        field("count", sorted.size)
        field("totalNanos", sorted.sum())
        field("meanNanos", if (sorted.isEmpty()) 0 else sorted.sum() / sorted.size)
        field("minNanos", sorted.firstOrNull() ?: 0)
        field("medianNanos", sorted.percentile(0.5))
        field("p95Nanos", sorted.percentile(0.95))
        field("maxNanos", sorted.lastOrNull() ?: 0)
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
