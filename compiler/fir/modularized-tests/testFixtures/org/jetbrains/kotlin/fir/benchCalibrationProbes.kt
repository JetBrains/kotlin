/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import java.io.File
import java.io.RandomAccessFile
import java.util.Random

/**
 * The calibration suite of [ModularizedTestInstrumentation].
 *
 * The purpose is to answer the question "how much of the difference between two runs is explained by the machine
 * itself?" - without it a faster CPU is indistinguishable from a machine without a monitoring agent.
 *
 * A single "cpu" number is not enough for that: the first version of this harness measured one serial integer
 * dependency chain, and it reported an M5 Max and an M2 Max as equal (ratio 0.98) while the published single-core
 * scores differ by 1.7x. Such a loop is limited by the latency of one multiply-add and saturates on any modern
 * core, so it says nothing about the core width, the branch predictor or the memory subsystem - which is what a
 * compiler workload is actually bound by.
 *
 * Hence one probe per *dimension*, so that the analysis can build a predicted ratio out of them and report the
 * residual (see `scripts/calibrate.py`):
 *
 * | dimension | probes | what it isolates |
 * |---|---|---|
 * | integer latency | `cpu` (= `cpuIntLatency`) | one dependent multiply-add chain: clock, nothing else |
 * | integer throughput | `cpuIntThroughput` | 8 independent chains: issue width / ILP |
 * | branches | `cpuBranch` | unpredictable branches: front end and branch predictor |
 * | floating point | `cpuFpu` | 4 independent multiply-add chains |
 * | memory latency | `memLatency32k/1m/8m/256m` | dependent-load latency per cache level, up to DRAM |
 * | memory bandwidth | `memBandwidthRead`, `memBandwidthCopy` | streaming throughput |
 * | allocation | `allocRate` | JVM allocation + young GC throughput |
 * | file syscalls | `fileOpenRead`, `fileOpenReadUnique`, `fileStat`, `fileCreateDelete`, `dirScan`, `diskWriteFsync` | exactly the operations an Endpoint Security extension hooks |
 * | disk bandwidth | `diskReadSequential` | the `read` path |
 * | process creation | `processExec` | `ES_EVENT_TYPE_AUTH_EXEC` is synchronous, so this is where an agent is most visible |
 *
 * Every probe is single-threaded, warmed up before being measured (otherwise the first iterations measure the
 * interpreter), and reported as a distribution - the median is what the analysis uses.
 */
internal object CalibrationProbes {

    /**
     * @param opsPerMeasurement how many operations one measurement covers, so that the analysis can report ns/op
     *   instead of the raw duration; `bytesPerMeasurement` does the same for the throughput probes.
     * @param aliases additional names the same numbers are reported under, to keep older run directories comparable.
     */
    class ProbeResult(
        val name: String,
        val kind: String,
        val unit: String,
        val opsPerMeasurement: Long,
        val bytesPerMeasurement: Long,
        val nanos: LongArray,
        val aliases: List<String> = emptyList(),
    )

    const val KIND_CPU = "cpu"
    const val KIND_MEMORY = "memory"
    const val KIND_DISK = "disk"
    const val KIND_PROCESS = "process"

    const val UNIT_NANOS_PER_OP = "ns/op"
    const val UNIT_BYTES_PER_SECOND = "bytes/s"

    /**
     * @param dir a scratch directory for the file probes, removed by the caller.
     * @param quick smaller working sets and fewer repetitions, for a run where the probes must not cost 15 seconds.
     * @param iterationsOverride per-probe repetition count, from `fir.bench.instrumentation.probes.<name>.iterations`.
     */
    fun run(dir: File, quick: Boolean, iterationsOverride: (String) -> Int?): List<ProbeResult> {
        val result = ArrayList<ProbeResult>()
        val scale = if (quick) 4 else 1

        fun iterations(name: String, default: Int): Int =
            iterationsOverride(name) ?: if (quick) Math.max(3, default / 2) else default

        // --- cpu ------------------------------------------------------------------------------------------------
        val intOps = 20_000_000L / scale
        // The body is deliberately identical to the very first version of this probe, so that the runs recorded
        // before the suite was introduced remain comparable
        val intLatency = measure(iterations("cpu", 20), warmup = 3) {
            var acc = 0L
            var x = 1L
            for (i in 0 until intOps) {
                x = x * 6364136223846793005L + 1442695040888963407L
                acc += x ushr 33
            }
            blackHole += acc
        }
        result += ProbeResult("cpu", KIND_CPU, UNIT_NANOS_PER_OP, intOps, 0, intLatency, aliases = listOf("cpuIntLatency"))

        result += ProbeResult(
            "cpuIntThroughput", KIND_CPU, UNIT_NANOS_PER_OP, intOps, 0,
            measure(iterations("cpuIntThroughput", 20), warmup = 3) { independentIntChains(intOps) }
        )

        val branchOps = 8_000_000L / scale
        val branchData = IntArray(1 shl 16).also { data ->
            val random = Random(20250909)
            for (i in data.indices) data[i] = random.nextInt()
        }
        result += ProbeResult(
            "cpuBranch", KIND_CPU, UNIT_NANOS_PER_OP, branchOps, 0,
            measure(iterations("cpuBranch", 20), warmup = 3) { unpredictableBranches(branchData, branchOps) }
        )

        result += ProbeResult(
            "cpuFpu", KIND_CPU, UNIT_NANOS_PER_OP, intOps, 0,
            measure(iterations("cpuFpu", 20), warmup = 3) { independentFpChains(intOps) }
        )

        // --- memory ---------------------------------------------------------------------------------------------
        // One dependent load per operation: the number is the latency of the level the working set fits into
        val chaseOps = 2_000_000L / scale
        for ([name, bytes] in MEM_LATENCY_WORKING_SETS) {
            if (!fitsIntoHeap(bytes)) continue
            val next = pointerChaseCycle(bytes)
            result += ProbeResult(
                name, KIND_MEMORY, UNIT_NANOS_PER_OP, chaseOps, 0,
                measure(iterations(name, 10), warmup = 2) {
                    var index = 0
                    for (i in 0 until chaseOps) index = next[index]
                    blackHole += index.toLong()
                }
            )
        }

        val streamBytes = if (quick) 16L * 1024 * 1024 else 64L * 1024 * 1024
        if (fitsIntoHeap(streamBytes.toInt())) {
            val source = LongArray((streamBytes / 8).toInt())
            result += ProbeResult(
                "memBandwidthRead", KIND_MEMORY, UNIT_BYTES_PER_SECOND, source.size.toLong(), streamBytes,
                measure(iterations("memBandwidthRead", 10), warmup = 2) {
                    var acc = 0L
                    for (i in source.indices) acc += source[i]
                    blackHole += acc
                }
            )
        }
        val copyBytes = streamBytes / 2
        if (fitsIntoHeap(copyBytes.toInt() * 2)) {
            val from = LongArray((copyBytes / 8).toInt())
            val to = LongArray(from.size)
            // Read + write, hence twice the array size of traffic
            result += ProbeResult(
                "memBandwidthCopy", KIND_MEMORY, UNIT_BYTES_PER_SECOND, from.size.toLong(), copyBytes * 2,
                measure(iterations("memBandwidthCopy", 10), warmup = 2) {
                    for (i in from.indices) to[i] = from[i]
                    blackHole += to[0]
                }
            )
        }

        // Compilation is allocation-bound as much as it is cpu-bound, and the allocation path is the one place
        // where the JVM and the OS (page faults, madvise) meet
        val allocBytes = if (quick) 64L * 1024 * 1024 else 256L * 1024 * 1024
        result += ProbeResult(
            "allocRate", KIND_MEMORY, UNIT_BYTES_PER_SECOND, allocBytes / ALLOCATION_SIZE, allocBytes,
            measure(iterations("allocRate", 10), warmup = 2) { allocate(allocBytes) }
        )

        // --- disk -----------------------------------------------------------------------------------------------
        val fileCount = if (quick) 128 else FILE_PROBE_COUNT
        val files = (0 until fileCount).map { index ->
            File(dir, "probe-$index.bin").also { it.writeBytes(ByteArray(4096) { index.toByte() }) }
        }
        result += ProbeResult(
            "fileOpenRead", KIND_DISK, UNIT_NANOS_PER_OP, 1, 0,
            files.measureEach { file -> RandomAccessFile(file, "r").use { it.read(ByteArray(4096)) } }
        )
        result += ProbeResult(
            "fileStat", KIND_DISK, UNIT_NANOS_PER_OP, 1, 0,
            files.measureEach { file -> file.length(); file.canRead() }
        )
        // Unlike the probe above, every path here is opened exactly once and the paths are spread over a deep
        // tree: an agent that caches its verdict per file cannot amortize it, so this is the probe expected to
        // expose a synchronously hooked `open`
        val uniqueFiles = createTree(File(dir, "unique"), fileCount)
        result += ProbeResult(
            "fileOpenReadUnique", KIND_DISK, UNIT_NANOS_PER_OP, 1, 0,
            uniqueFiles.measureEach { file -> RandomAccessFile(file, "r").use { it.read(ByteArray(4096)) } }
        )
        result += ProbeResult(
            "fileCreateDelete", KIND_DISK, UNIT_NANOS_PER_OP, 1, 0,
            (0 until fileCount).toList().measureEach { index ->
                val file = File(dir, "created-$index.bin")
                file.writeBytes(ByteArray(64))
                file.delete()
            }
        )
        // What the Gradle configuration phase does all the time
        val treeRoot = File(dir, "unique")
        result += ProbeResult(
            "dirScan", KIND_DISK, UNIT_NANOS_PER_OP, uniqueFiles.size.toLong(), 0,
            measure(iterations("dirScan", 10), warmup = 2) { blackHole += scanTree(treeRoot) }
        )
        result += diskReadSequential(dir, quick, iterations("diskReadSequential", 5))
        // An fsync is bimodal (the drive's write cache), so it needs many more samples than the other probes to
        // produce a stable median; at ~20 us each this is still only a few milliseconds
        result += diskWriteFsync(dir, iterations("diskWriteFsync", if (quick) 64 else 256))

        // --- process --------------------------------------------------------------------------------------------
        result += ProbeResult(
            "processExec", KIND_PROCESS, UNIT_NANOS_PER_OP, 1, 0,
            (0 until iterations("processExec", 30)).toList().measureEach {
                try {
                    ProcessBuilder("/usr/bin/true").start().waitFor()
                } catch (e: Exception) {
                    // The probe is best effort only
                }
            }
        )
        return result
    }

    private val MEM_LATENCY_WORKING_SETS = listOf(
        "memLatency32k" to 32 * 1024,
        "memLatency1m" to 1024 * 1024,
        "memLatency8m" to 8 * 1024 * 1024,
        "memLatency256m" to 256 * 1024 * 1024,
    )

    private const val FILE_PROBE_COUNT = 512
    private const val ALLOCATION_SIZE = 64

    /** The probes must never be the reason a benchmark run dies with an OOM. */
    private fun fitsIntoHeap(bytes: Int): Boolean = bytes.toLong() * 4 < Runtime.getRuntime().maxMemory()

    @Volatile
    private var blackHole: Long = 0

    @Volatile
    private var blackHoleDouble: Double = 0.0

    private val allocationSink = arrayOfNulls<Any>(1024)

    private inline fun measure(iterations: Int, warmup: Int, body: () -> Unit): LongArray {
        for (i in 0 until warmup) body()
        val result = LongArray(Math.max(1, iterations))
        for (i in result.indices) {
            val start = System.nanoTime()
            body()
            result[i] = System.nanoTime() - start
        }
        return result
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

    private fun independentIntChains(ops: Long) {
        var x0 = 1L; var x1 = 2L; var x2 = 3L; var x3 = 4L
        var x4 = 5L; var x5 = 6L; var x6 = 7L; var x7 = 8L
        for (i in 0 until ops / 8) {
            x0 = x0 * 6364136223846793005L + 1442695040888963407L
            x1 = x1 * 6364136223846793005L + 1442695040888963407L
            x2 = x2 * 6364136223846793005L + 1442695040888963407L
            x3 = x3 * 6364136223846793005L + 1442695040888963407L
            x4 = x4 * 6364136223846793005L + 1442695040888963407L
            x5 = x5 * 6364136223846793005L + 1442695040888963407L
            x6 = x6 * 6364136223846793005L + 1442695040888963407L
            x7 = x7 * 6364136223846793005L + 1442695040888963407L
        }
        blackHole += x0 + x1 + x2 + x3 + x4 + x5 + x6 + x7
    }

    private fun unpredictableBranches(data: IntArray, ops: Long) {
        val mask = data.size - 1
        var acc = 0L
        var index = 0
        for (i in 0 until ops) {
            // The data is random, so the branch is unpredictable by construction
            acc += if (data[index] > 0) 1 else -1
            index = (index + 1) and mask
        }
        blackHole += acc
    }

    private fun independentFpChains(ops: Long) {
        var a0 = 1.0; var a1 = 1.5; var a2 = 2.0; var a3 = 2.5
        for (i in 0 until ops / 4) {
            a0 = a0 * 1.0000001 + 0.5
            a1 = a1 * 1.0000001 + 0.5
            a2 = a2 * 1.0000001 + 0.5
            a3 = a3 * 1.0000001 + 0.5
        }
        blackHoleDouble += a0 + a1 + a2 + a3
    }

    /**
     * A single random cycle over the whole working set: every load depends on the previous one, so the loop cannot
     * be pipelined and the measurement is the latency of the memory level the array fits into.
     */
    private fun pointerChaseCycle(bytes: Int): IntArray {
        val size = bytes / 4
        val permutation = IntArray(size) { it }
        val random = Random(20250909)
        for (i in size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = permutation[i]
            permutation[i] = permutation[j]
            permutation[j] = tmp
        }
        val next = IntArray(size)
        for (i in 0 until size - 1) next[permutation[i]] = permutation[i + 1]
        next[permutation[size - 1]] = permutation[0]
        return next
    }

    private fun allocate(bytes: Long) {
        var index = 0
        var allocated = 0L
        while (allocated < bytes) {
            // Store the object so that it cannot be scalar-replaced, but keep the live set tiny
            allocationSink[index and (allocationSink.size - 1)] = ByteArray(ALLOCATION_SIZE)
            allocated += ALLOCATION_SIZE
            index++
        }
        blackHole += index.toLong()
    }

    private fun createTree(root: File, fileCount: Int): List<File> {
        val result = ArrayList<File>(fileCount)
        val leafCount = Math.min(64, fileCount)
        val filesPerLeaf = Math.max(1, fileCount / leafCount)
        for (leaf in 0 until leafCount) {
            val leafDir = File(root, "${leaf / 8}/${leaf % 8}").also { it.mkdirs() }
            for (index in 0 until filesPerLeaf) {
                result += File(leafDir, "unique-$leaf-$index.bin").also { it.writeBytes(ByteArray(4096)) }
            }
        }
        return result
    }

    private fun scanTree(root: File): Long {
        var acc = 0L
        val children = root.listFiles() ?: return 0
        for (child in children) {
            acc += if (child.isDirectory) scanTree(child) else child.length()
        }
        return acc
    }

    private fun diskReadSequential(dir: File, quick: Boolean, iterations: Int): ProbeResult {
        val bytes = if (quick) 16L * 1024 * 1024 else 64L * 1024 * 1024
        val file = File(dir, "sequential.bin")
        val buffer = ByteArray(1024 * 1024)
        RandomAccessFile(file, "rw").use { output ->
            var written = 0L
            while (written < bytes) {
                output.write(buffer)
                written += buffer.size
            }
        }
        val nanos = measure(iterations, warmup = 1) {
            RandomAccessFile(file, "r").use { input ->
                while (true) {
                    if (input.read(buffer) <= 0) break
                }
            }
        }
        file.delete()
        // The page cache is warm here, so this is the `read` syscall path rather than the medium itself - which is
        // exactly what a monitoring agent intercepts
        return ProbeResult("diskReadSequential", KIND_DISK, UNIT_BYTES_PER_SECOND, bytes / buffer.size, bytes, nanos)
    }

    private fun diskWriteFsync(dir: File, iterations: Int): ProbeResult {
        val file = File(dir, "fsync.bin")
        val buffer = ByteArray(4096)
        val nanos = RandomAccessFile(file, "rw").use { output ->
            measure(iterations, warmup = 2) {
                output.seek(0)
                output.write(buffer)
                output.fd.sync()
            }
        }
        file.delete()
        return ProbeResult("diskWriteFsync", KIND_DISK, UNIT_NANOS_PER_OP, 1, buffer.size.toLong(), nanos)
    }
}
