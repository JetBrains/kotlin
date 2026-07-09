/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.merger

import com.github.luben.zstd.ZstdCompressCtx
import com.github.luben.zstd.ZstdDecompressCtx
import org.jetbrains.kotlin.library.KlibConstants
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.outputStream

/**
 * Prototype utility for KT-87204: bundles a set of unpackaged (directory) klibs into a single
 * zstd-compressed artifact and extracts an individual klib back out of it. zstd is the fixed codec.
 *
 * Container layout — a self-describing header up front, then the frames:
 * ```
 * [magic: int][version: byte][indexLength: int][index][reference frame][other frames ...]
 * ```
 * The leading [MAGIC] + [FORMAT_VERSION] identify the format without seeking. The index (the reference
 * name and a table of entries with each frame's absolute byte offset and sizes) precedes the frames, so a
 * reader parses the whole directory from the head and can then seek straight to any single frame. Each
 * `frame` is one independently decompressible zstd frame whose uncompressed payload is a deterministic
 * serialization of one klib directory.
 *
 * ## Cross-klib deduplication (the "klib of klibs" win)
 *
 * When [packageKlibDirectories] is given a [referenceKlibName][packageKlibDirectories], that klib's payload is
 * used as a shared dictionary: every other frame is compressed against it (with long-distance matching so
 * the whole dictionary is reachable), and its frame is placed first (right after the header) since every
 * other klib needs it to decompress. Because a multiplatform publication's per-target klibs are
 * near-identical, this shrinks the non-reference frames to small deltas while keeping each frame
 * independently decompressible — extraction needs the reference frame plus the target frame (two small
 * decompressions, bounded memory), not the whole archive.
 *
 * Without a reference, frames are fully independent (cheap extraction, but only per-klib compression gains).
 */
object KlibMerger {
    /** Magic marker at offset 0: ASCII "KZLB". */
    private const val MAGIC = 0x4B5A4C42

    /** On-disk container format version. */
    private const val FORMAT_VERSION = 1

    /** Fixed-size header prefix preceding the index: magic (4) + version (1) + indexLength (4). */
    private const val HEADER_PREFIX_SIZE = 9

    /** Default zstd compression level. */
    const val DEFAULT_LEVEL = 19

    /** Window log (2^27 = 128 MB) enabling long-distance matching; defaulted on for the reference scheme. */
    private const val REFERENCE_WINDOW_LOG = 27

    /** One entry in the artifact index: a named klib frame and its byte range. */
    data class Entry(
        val name: String,
        val offset: Long,
        val compressedSize: Long,
        val uncompressedSize: Long,
        /** True if this frame was compressed against the archive's reference klib as a dictionary. */
        val usesReference: Boolean = false,
    )

    /**
     * Packs [klibs] (each an unpackaged klib directory) into a single zstd artifact at [output].
     * The klib name is derived from its directory name.
     *
     * If [referenceKlibName] is set, that klib's payload becomes a shared dictionary for every other frame
     * (enabling cross-klib deduplication); it must name one of the [klibs].
     *
     * Frames are buffered in memory so the index (with offsets) can be written ahead of them; this is fine
     * for klib-sized inputs.
     */
    fun packageKlibDirectories(
        klibs: List<Path>,
        output: Path,
        referenceKlibName: String? = null,
    ) {
        output.toAbsolutePath().parent?.let { Files.createDirectories(it) }

        val names = klibs.map { klibName(it) }
        require(names.toSet().size == names.size) { "Duplicate klib names while packing: $names" }
        if (referenceKlibName != null) {
            require(referenceKlibName in names) { "Reference klib '$referenceKlibName' is not among the inputs" }
        }
        klibs.forEach { require(it.isDirectory()) { "Only unpackaged (directory) klibs are supported in the prototype: $it" } }

        // The reference payload (if any) is the shared dictionary for every other frame.
        val referencePayload: ByteArray? =
            referenceKlibName?.let { serializeKlib(klibs[names.indexOf(it)]) }

        // Compress every klib to a frame, ordered on disk with the reference first (right after the header).
        val order = if (referenceKlibName != null) {
            val referenceIndex = names.indexOf(referenceKlibName)
            listOf(referenceIndex) + klibs.indices.filter { it != referenceIndex }
        } else {
            klibs.indices.toList()
        }
        val frames = order.map { i ->
            val name = names[i]
            val payload = serializeKlib(klibs[i])
            val usesReference = referencePayload != null && name != referenceKlibName
            val bytes = compressFrame(payload, dictionary = if (usesReference) referencePayload else null)
            PackedFrame(name, bytes, payload.size.toLong(), usesReference)
        }

        // Offsets depend on the header (and thus the index) size. The index's byte length is independent of
        // the offset values (offsets are fixed-width), so size it once with placeholders, then fill real offsets.
        fun indexBytes(offsets: LongArray): ByteArray = buildIndex(
            ArchiveIndex(referenceKlibName, frames.mapIndexed { i, f ->
                Entry(f.name, offsets[i], f.bytes.size.toLong(), f.uncompressedSize, f.usesReference)
            })
        )

        val sizedIndex = indexBytes(LongArray(frames.size))
        var offset = (HEADER_PREFIX_SIZE + sizedIndex.size).toLong()
        val offsets = LongArray(frames.size)
        for (i in frames.indices) {
            offsets[i] = offset
            offset += frames[i].bytes.size
        }
        val index = indexBytes(offsets)
        check(index.size == sizedIndex.size) { "Index length changed between sizing and writing" }

        output.outputStream().buffered().use { raw ->
            val out = DataOutputStream(raw)
            out.writeInt(MAGIC)
            out.writeByte(FORMAT_VERSION)
            out.writeInt(index.size)
            out.write(index)
            for (frame in frames) out.write(frame.bytes)
            out.flush()
        }
    }

    /**
     * Extracts the klib named [name] from [archive] into [destination] (as a directory klib).
     * Reads and decompresses only that klib's frame — plus the reference frame if [name] was
     * compressed against the reference.
     */
    fun extractKlib(archive: Path, name: String, destination: Path) {
        val index = readArchiveIndex(archive)
        val entry = index.entries.firstOrNull { it.name == name }
            ?: error("No klib named '$name' in $archive")

        // Reference-compressed frames need the reference payload as their dictionary.
        val dictionary: ByteArray? = if (entry.usesReference) {
            val referenceName = index.referenceName
                ?: error("'$name' is marked reference-compressed but $archive declares no reference")
            val referenceEntry = index.entries.first { it.name == referenceName }
            decodeFrame(archive, referenceEntry, dictionary = null)
        } else {
            null
        }

        val payload = decodeFrame(archive, entry, dictionary)
        deserializeKlib(payload, destination)
    }

    /** Reads only the header/index of [archive] without decompressing any frame. */
    fun listEntries(archive: Path): List<Entry> = readArchiveIndex(archive).entries

    /** The name of the reference klib used as a shared dictionary, or null if the archive has none. */
    fun referenceKlibName(archive: Path): String? = readArchiveIndex(archive).referenceName

    private fun decodeFrame(archive: Path, entry: Entry, dictionary: ByteArray?): ByteArray {
        val frame = ByteArray(entry.compressedSize.toInt())
        RandomAccessFile(archive.toFile(), "r").use { raf ->
            raf.seek(entry.offset)
            raf.readFully(frame)
        }
        return decompressFrame(frame, entry.uncompressedSize.toInt(), dictionary)
    }

    private fun compressFrame(data: ByteArray, dictionary: ByteArray?): ByteArray =
        ZstdCompressCtx().use { ctx ->
            ctx.setLevel(DEFAULT_LEVEL)
            ctx.setLong(REFERENCE_WINDOW_LOG)
            dictionary?.let { ctx.loadDict(it) }
            ctx.compress(data)
        }

    /**
     * Decompresses one zstd frame; [dictionary] must match the one used to compress it. No window-log max is
     * set: frames are packed with windowLog <= 27 (zstd's default decompression window limit).
     */
    private fun decompressFrame(frame: ByteArray, uncompressedSize: Int, dictionary: ByteArray?): ByteArray =
        ZstdDecompressCtx().use { ctx ->
            dictionary?.let { ctx.loadDict(it) }
            ctx.decompress(frame, uncompressedSize)
        }

    private fun klibName(klib: Path): String =
        klib.name.removeSuffix(KlibConstants.KLIB_FILE_EXTENSION_WITH_DOT)

    /**
     * Serializes a klib directory tree into a single deterministic byte payload:
     * `count` followed by `(relativePath, length, bytes)` per file, ordered by path.
     *
     * `internal` so measurement/benchmark code in this module can reuse the exact payload format.
     */
    internal fun serializeKlib(klib: Path): ByteArray {
        val rootFile = klib.toFile()
        val files = rootFile.walkTopDown()
            .filter { it.isFile }
            .map { it to it.relativeTo(rootFile).path.replace('\\', '/') }
            .sortedBy { it.second } // deterministic ordering for reproducible frames
            .toList()
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { data ->
            data.writeInt(files.size)
            for ((file, relativePath) in files) {
                val content = file.readBytes()
                data.writeUTF(relativePath)
                data.writeInt(content.size)
                data.write(content)
            }
        }
        return buffer.toByteArray()
    }

    /** Materializes a payload produced by [serializeKlib] as a directory klib under [destination]. */
    private fun deserializeKlib(payload: ByteArray, destination: Path) {
        val root = destination.normalize()
        Files.createDirectories(root)
        DataInputStream(ByteArrayInputStream(payload)).use { data ->
            val count = data.readInt()
            repeat(count) {
                val relativePath = data.readUTF()
                val content = ByteArray(data.readInt())
                data.readFully(content)
                val target = root.resolve(relativePath).normalize()
                require(target.startsWith(root)) { "Illegal entry path escaping destination: $relativePath" }
                target.parent?.let { Files.createDirectories(it) }
                Files.write(target, content)
            }
        }
    }

    private class PackedFrame(
        val name: String,
        val bytes: ByteArray,
        val uncompressedSize: Long,
        val usesReference: Boolean,
    )

    private class ArchiveIndex(val referenceName: String?, val entries: List<Entry>)

    private fun buildIndex(index: ArchiveIndex): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { data ->
            data.writeInt(index.entries.size)
            data.writeUTF(index.referenceName ?: "")
            for (e in index.entries) {
                data.writeUTF(e.name)
                data.writeLong(e.offset)
                data.writeLong(e.compressedSize)
                data.writeLong(e.uncompressedSize)
                data.writeBoolean(e.usesReference)
            }
        }
        return bytes.toByteArray()
    }

    private fun readArchiveIndex(archive: Path): ArchiveIndex {
        RandomAccessFile(archive.toFile(), "r").use { raf ->
            require(raf.length() >= HEADER_PREFIX_SIZE) { "Not a klib archive (too small): $archive" }
            val magic = raf.readInt()
            require(magic == MAGIC) { "Not a klib archive (bad magic): $archive" }
            val version = raf.readUnsignedByte()
            require(version == FORMAT_VERSION) { "Unsupported klib archive version $version (expected $FORMAT_VERSION): $archive" }

            val indexLength = raf.readInt()
            require(indexLength >= 0 && HEADER_PREFIX_SIZE + indexLength.toLong() <= raf.length()) {
                "Corrupted klib archive index: $archive"
            }
            val bytes = ByteArray(indexLength)
            raf.readFully(bytes)
            return parseIndex(bytes)
        }
    }

    private fun parseIndex(bytes: ByteArray): ArchiveIndex {
        DataInputStream(ByteArrayInputStream(bytes)).use { data ->
            val count = data.readInt()
            val referenceName = data.readUTF().ifEmpty { null }
            val entries = (0 until count).map {
                Entry(
                    name = data.readUTF(),
                    offset = data.readLong(),
                    compressedSize = data.readLong(),
                    uncompressedSize = data.readLong(),
                    usesReference = data.readBoolean(),
                )
            }
            return ArchiveIndex(referenceName, entries)
        }
    }
}
