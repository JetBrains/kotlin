/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.merger

import com.github.luben.zstd.ZstdCompressCtx
import com.github.luben.zstd.ZstdDictTrainer
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

/**
 * Exploratory measurement for KT-87204 (not part of the regular suite — skipped unless
 * `-Dcoroutines.klibs.dir=<dir of .klib files>` is provided).
 *
 * Compares, for a set of real (packaged) klibs:
 *  - the current baseline (sum of the gzip/deflate `.klib` file sizes),
 *  - the [KlibMerger] bundle (independent zstd frame per klib), and
 *  - a single combined zstd frame over all payloads (the cross-klib dedup ceiling).
 *
 * The gap between the last two quantifies how much of the "klib of klibs" win the independent-frame
 * design leaves on the table (i.e. the motivation for the reference-klib scheme).
 */
class KlibMergerCoroutinesMeasurementTest {
    @Test
    fun measure(@TempDir tmp: Path) {
        val dirProp = System.getProperty("coroutines.klibs.dir")
        Assumptions.assumeTrue(!dirProp.isNullOrBlank(), "Set -Dcoroutines.klibs.dir=<dir with .klib files> to run")

        val klibFiles = Paths.get(dirProp!!).listDirectoryEntries("*.klib").sorted()
        Assumptions.assumeTrue(klibFiles.isNotEmpty(), "No .klib files found in $dirProp")

        val baseline = klibFiles.sumOf { it.fileSize() }

        val dirs = klibFiles.mapIndexed { index, file -> unzipKlib(file, tmp.resolve("klib_$index")) }

        // The real KlibMerger output: one independent zstd frame per klib + index.
        val bundle = tmp.resolve("bundle.klibz")
        KlibMerger.packageKlibDirectories(dirs, bundle)
        val independentFrames = bundle.fileSize()

        // References computed from the exact same payload serialization.
        val payloads = dirs.map { KlibMerger.serializeKlib(it) }
        val rawTotal = payloads.sumOf { it.size.toLong() }
        val combined = ByteArrayOutputStream().apply { payloads.forEach { write(it) } }.toByteArray()

        // Cross-klib dedup only works if the compression window reaches across klib boundaries:
        // enable long-distance matching with a window (2^27 = 128 MB) larger than the whole payload.
        val combinedLdm = compress(combined, HIGH_LEVEL, LONG_WINDOW_LOG).size.toLong()
        // Independent frames at a high level: the per-klib gain, still no cross-klib sharing.
        val perFrameHigh = payloads.sumOf { compress(it, HIGH_LEVEL).size.toLong() }

        // Shared-dictionary schemes: independent (cheaply extractable) frames that dedup against a
        // dictionary stored once. Sweep trained-dict sizes; also use the first klib as a raw dictionary.
        fun trainedDictTotal(dictSize: Int): Long? = runCatching {
            val trained = ZstdDictTrainer(rawTotal.toInt() + (1 shl 20), dictSize)
                .apply { payloads.forEach { addSample(it) } }.trainSamples()
            compress(trained, HIGH_LEVEL, LONG_WINDOW_LOG).size.toLong() +
                payloads.sumOf { compress(it, HIGH_LEVEL, LONG_WINDOW_LOG, trained).size.toLong() }
        }.getOrNull()
        val dict1 = trainedDictTotal(1 shl 20)
        val dict4 = trainedDictTotal(4 shl 20)
        val dict16 = trainedDictTotal(16 shl 20)
        // First klib used as a raw-content dictionary for the rest: klib[0] stored whole, others as deltas.
        val firstAsDict = compress(payloads[0], HIGH_LEVEL, LONG_WINDOW_LOG).size.toLong() +
            payloads.drop(1).sumOf { compress(it, HIGH_LEVEL, LONG_WINDOW_LOG, payloads[0]).size.toLong() }
        // The same idea through the real KlibMerger reference-klib API (incl. container/index overhead).
        val referenceBundle = tmp.resolve("bundle-ref.klibz")
        KlibMerger.packageKlibDirectories(dirs, referenceBundle, referenceKlibName = dirs.first().fileName.toString())
        val referenceScheme = referenceBundle.fileSize()

        fun mb(bytes: Long) = "%.2f MB".format(bytes / 1_048_576.0)
        fun pct(bytes: Long) = "%.1f%%".format(100.0 * bytes / baseline)
        fun row(bytes: Long?) = if (bytes == null) "n/a" else "${mb(bytes)}  (${pct(bytes)})"

        val report = buildString {
            appendLine("=== klib-of-klibs compression measurement (${klibFiles.size} klibs) ===")
            klibFiles.forEach { appendLine("  input: ${it.fileName} (${mb(it.fileSize())})") }
            appendLine("-".repeat(74))
            appendLine("baseline: sum of .klib files (gzip/deflate)       : ${row(baseline)}")
            appendLine("raw uncompressed payload total                    : ${mb(rawTotal)}")
            appendLine("independent frames, level ${KlibMerger.DEFAULT_LEVEL} (KlibMerger)         : ${row(independentFrames)}")
            appendLine("independent frames, level $HIGH_LEVEL                   : ${row(perFrameHigh)}")
            appendLine("shared trained-dict frames, 1 MB, level $HIGH_LEVEL      : ${row(dict1)}")
            appendLine("shared trained-dict frames, 4 MB, level $HIGH_LEVEL      : ${row(dict4)}")
            appendLine("shared trained-dict frames, 16 MB, level $HIGH_LEVEL     : ${row(dict16)}")
            appendLine("first-klib-as-dict frames, level $HIGH_LEVEL             : ${row(firstAsDict)}")
            appendLine("reference-klib scheme via KlibMerger (LDM)        : ${row(referenceScheme)}")
            appendLine("single combined frame, level $HIGH_LEVEL + long window  : ${row(combinedLdm)}  <- cross-klib dedup ceiling")
        }

        val out = Paths.get(System.getProperty("java.io.tmpdir"), "klib-merger-measurement.txt")
        out.writeText(report)
        println(report)
        println("(report also written to $out)")
    }

    /** Compresses [data] into one zstd frame, optionally with long-distance matching and/or a dictionary. */
    private fun compress(data: ByteArray, level: Int, windowLog: Int? = null, dict: ByteArray? = null): ByteArray =
        ZstdCompressCtx().use { ctx ->
            ctx.setLevel(level)
            if (windowLog != null) ctx.setLong(windowLog)
            if (dict != null) ctx.loadDict(dict)
            ctx.compress(data)
        }

    private fun unzipKlib(klibFile: Path, target: Path): Path {
        Files.createDirectories(target)
        ZipInputStream(klibFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val extracted = target.resolve(entry.name).normalize()
                    require(extracted.startsWith(target)) { "Illegal zip entry path: ${entry.name}" }
                    extracted.parent?.let { Files.createDirectories(it) }
                    Files.newOutputStream(extracted).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return target
    }

    private companion object {
        const val HIGH_LEVEL = 19
        const val LONG_WINDOW_LOG = 27 // 2^27 = 128 MB window; enables long-distance matching
    }
}
