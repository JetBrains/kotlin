/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import sun.misc.Unsafe

class ThreadSafeDisposableHelper<T>(private val _create: () -> T, private val _dispose: (T) -> Unit) {
    var holder: T? = null
        private set

    private var counter = 0
    private val lock = Any()

    fun create() {
        synchronized(lock) {
            if (counter++ == 0) {
                check(holder == null)
                holder = _create()
            }
        }
    }

    fun dispose() {
        synchronized(lock) {
            if (--counter == 0) {
                _dispose(holder!!)
                holder = null
            }
        }
    }

    inline fun <R> usingDisposable(block: () -> R): R {
        create()
        return try {
            block()
        } finally {
            dispose()
        }
    }
}

@PublishedApi
internal val allocatorDisposeHelper = ThreadSafeDisposableHelper({ NativeMemoryAllocator() }, { it.freeAll() })

inline fun <R> usingNativeMemoryAllocator(block: () -> R) = allocatorDisposeHelper.usingDisposable(block)

val nativeMemoryAllocator: NativeMemoryAllocator
    get() = allocatorDisposeHelper.holder ?: error("Native memory allocator hasn't been created")

// 256 buckets for sizes <= 2048 padded to 8
// 256 buckets for sizes <= 64KB padded to 256
// 256 buckets for sizes <= 1MB padded to 4096
private const val ChunkBucketSize = 256
// Alignments are such that overhead is approx 10%.
private const val SmallChunksSizeAlignment = 8
private const val MediumChunksSizeAlignment = 256
private const val BigChunksSizeAlignment = 4096
private const val MaxSmallSize = ChunkBucketSize * SmallChunksSizeAlignment
private const val MaxMediumSize = ChunkBucketSize * MediumChunksSizeAlignment
private const val MaxBigSize = ChunkBucketSize * BigChunksSizeAlignment
private const val ChunkHeaderSize = 3 * Int.SIZE_BYTES // chunk size + raw chunk ref + alignment hop size.

private const val RawChunkSizeBits = 22 // 4MB
private const val RawChunkSize = 1L shl RawChunkSizeBits
private const val ChunkSizeAlignmentBits = 3 // All chunk sizes are aligned to at least 8.
private const val RawChunkOffsetBits = RawChunkSizeBits - ChunkSizeAlignmentBits
private const val MinChunkSize = 8
private const val MaxRawChunksCount = 1024 // 4GB in total.

class NativeMemoryAllocator {
    companion object {
        fun init() = allocatorDisposeHelper.create()
        fun dispose() = allocatorDisposeHelper.dispose()
    }

    private fun alignUp(x: Long, align: Int) = (x + align - 1) and (align - 1).toLong().inv()
    private fun alignUp(x: Int, align: Int) = (x + align - 1) and (align - 1).inv()

    private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }

    private val longArrayBaseOffset = unsafe.arrayBaseOffset(LongArray::class.java).toLong()

    private val rawOffsetFieldOffset = unsafe.objectFieldOffset(this::class.java.getDeclaredField("rawOffset"))

    @JvmInline
    private value class ChunkRef(val value: Int) {
        val offset get() = (value and ((1 shl RawChunkOffsetBits) - 1)) shl ChunkSizeAlignmentBits

        val index get() = (value ushr RawChunkOffsetBits) - 1

        companion object {
            init {
                // Ensure that pair (index, offset) fits in 32-bit integer.
                check(MaxRawChunksCount < 1L shl (32 - RawChunkOffsetBits))
            }

            fun encode(index: Int, offset: Int) = ChunkRef(((index + 1) shl RawChunkOffsetBits) or (offset ushr ChunkSizeAlignmentBits))

            val Invalid = ChunkRef(0)
        }
    }

    // Timestamps here solve the ABA problem.
    @JvmInline
    private value class ChunkRefWithTimestamp(val value: Long) {
        val chunkRef get() = ChunkRef(value.toInt())

        val timestamp get() = (value ushr 32).toInt()

        companion object {
            fun encode(chunkRef: Int, timestamp: Int) = ChunkRefWithTimestamp(chunkRef.toLong() or (timestamp.toLong() shl 32))
        }
    }

    private fun getChunkSize(chunk: Long) = unsafe.getInt(chunk)
    private fun setChunkSize(chunk: Long, size: Int) = unsafe.putInt(chunk, size)

    private fun getChunkRef(chunk: Long): ChunkRef = ChunkRef(unsafe.getInt(chunk + Int.SIZE_BYTES /* skip chunk size */))
    private fun setChunkRef(chunk: Long, chunkRef: ChunkRef) = unsafe.putInt(chunk + Int.SIZE_BYTES /* skip chunk size */, chunkRef.value)

    private val smallChunks = LongArray(ChunkBucketSize)
    private val mediumChunks = LongArray(ChunkBucketSize)
    private val bigChunks = LongArray(ChunkBucketSize)

    // Chunk layout: [chunk size, raw chunk ref,...padding...,diff to start,aligned data start,.....data.....]
    fun alloc(size: Long, align: Int): Long {
        val totalChunkSize = ChunkHeaderSize + size + align
        val chunkStart = when {
            totalChunkSize <= MaxSmallSize -> allocFromFreeList(totalChunkSize.toInt(), SmallChunksSizeAlignment, smallChunks)
            totalChunkSize <= MaxMediumSize -> allocFromFreeList(totalChunkSize.toInt(), MediumChunksSizeAlignment, mediumChunks)
            totalChunkSize <= MaxBigSize -> allocFromFreeList(totalChunkSize.toInt(), BigChunksSizeAlignment, bigChunks)
            else -> unsafe.allocateMemory(totalChunkSize).also {
                // The actual size is not used. Just put value bigger than the biggest threshold.
                setChunkSize(it, Int.MAX_VALUE)
            }
        }
        val chunkWithSkippedHeader = chunkStart + ChunkHeaderSize
        val alignedPtr = alignUp(chunkWithSkippedHeader, align)
        unsafe.putInt(alignedPtr - Int.SIZE_BYTES, (alignedPtr - chunkStart).toInt())
        return alignedPtr
    }

    fun free(mem: Long) {
        val chunkStart = mem - unsafe.getInt(mem - Int.SIZE_BYTES)
        val chunkSize = getChunkSize(chunkStart)
        when {
            chunkSize <= MaxSmallSize -> freeToFreeList(chunkSize, SmallChunksSizeAlignment, smallChunks, chunkStart)
            chunkSize <= MaxMediumSize -> freeToFreeList(chunkSize, MediumChunksSizeAlignment, mediumChunks, chunkStart)
            chunkSize <= MaxBigSize -> freeToFreeList(chunkSize, BigChunksSizeAlignment, bigChunks, chunkStart)
            else -> unsafe.freeMemory(chunkStart)
        }
    }

    private val rawChunks = LongArray(MaxRawChunksCount)

    private fun ChunkRef.dereference() = rawChunks[index] + offset

    private val rawChunksLock = Any()

    @Volatile
    private var rawOffset = 0L

    private fun allocRaw(size: Int): Long {
        require(size % MinChunkSize == 0) { "Sizes should be multiples of $MinChunkSize" }
        while (true) {
            val offset = rawOffset
            val remainedInCurChunk = (alignUp(offset + 1, RawChunkSize.toInt()) - offset).toInt()
            val newOffset = offset + if (remainedInCurChunk >= size) size else remainedInCurChunk + size
            if (!unsafe.compareAndSwapLong(this, rawOffsetFieldOffset, offset, newOffset)) continue
            val dataStartOffset = newOffset - size
            val rawChunkIndex = (dataStartOffset ushr RawChunkSizeBits).toInt()
            val rawChunkOffset = (dataStartOffset and (RawChunkSize - 1)).toInt()
            var rawChunk = rawChunks[rawChunkIndex]
            if (rawChunk == 0L) {
                synchronized(rawChunksLock) {
                    rawChunk = rawChunks[rawChunkIndex]
                    if (rawChunk == 0L) {
                        rawChunk = unsafe.allocateMemory(RawChunkSize)
                        rawChunks[rawChunkIndex] = rawChunk
                    }
                }
            }
            val ptr = rawChunk + rawChunkOffset
            setChunkRef(ptr, ChunkRef.encode(rawChunkIndex, rawChunkOffset))
            return ptr
        }
    }

    private fun allocFromFreeList(size: Int, align: Int, freeList: LongArray): Long {
        val paddedSize = alignUp(size, align)
        val index = paddedSize / align - 1
        val ptr: Long
        while (true) {
            val chunkRefWithTimestamp = ChunkRefWithTimestamp(freeList[index])
            val chunkRef = chunkRefWithTimestamp.chunkRef
            if (chunkRef == ChunkRef.Invalid) {
                ptr = allocRaw(paddedSize)
                break
            } else {
                val chunk = chunkRef.dereference()
                val nextChunkRef = unsafe.getInt(chunk)
                val nextChunkRefWithTimestamp = ChunkRefWithTimestamp.encode(nextChunkRef, chunkRefWithTimestamp.timestamp + 1)
                if (unsafe.compareAndSwapLong(freeList, longArrayBaseOffset + index * Long.SIZE_BYTES,
                                chunkRefWithTimestamp.value, nextChunkRefWithTimestamp.value)) {
                    ptr = chunk
                    break
                }
            }
        }
        setChunkSize(ptr, paddedSize)
        return ptr
    }

    private fun freeToFreeList(paddedSize: Int, align: Int, freeList: LongArray, chunk: Long) {
        require(paddedSize > 0 && paddedSize % align == 0)
        val index = paddedSize / align - 1
        do {
            val nextChunkRefWithTimestamp = ChunkRefWithTimestamp(freeList[index])
            unsafe.putInt(chunk, nextChunkRefWithTimestamp.chunkRef.value)
            val chunkRef = getChunkRef(chunk)
            val chunkRefWithTimestamp = ChunkRefWithTimestamp.encode(chunkRef.value, nextChunkRefWithTimestamp.timestamp + 1)
        } while (!unsafe.compareAndSwapLong(freeList, longArrayBaseOffset + index * Long.SIZE_BYTES,
                        nextChunkRefWithTimestamp.value, chunkRefWithTimestamp.value))
    }

    internal fun freeAll() {
        for (i in 0 until ChunkBucketSize) {
            smallChunks[i] = 0L
            mediumChunks[i] = 0L
            bigChunks[i] = 0L
        }
        for (index in rawChunks.indices) {
            val rawChunk = rawChunks[index]
            if (rawChunk != 0L)
                unsafe.freeMemory(rawChunk)
            rawChunks[index] = 0L
        }
        rawOffset = 0L
    }
}