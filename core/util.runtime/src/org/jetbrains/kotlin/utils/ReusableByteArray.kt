/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val collectStats = System.getProperty("org.jetbrains.kotlin.utils.ReusableByteArray.stats") == "true"

/**
 * Reusable byte array with reference counted tracking.
 * The primary constructor wraps byte array into fresh reusable byte array that is never reused,
 * so it is not necessarily to call [release] on it afterwards.
 */
class ReusableByteArray @JvmOverloads constructor(val bytes: ByteArray, recordStats: Boolean = false) {
    internal constructor(size: Int, recordStats: Boolean) : this(ByteArray(size), recordStats)

    var size: Int = bytes.size
        internal set
    private val refCount = AtomicInteger(1)
    private var readerCache: Any? = null // Caches reference to ClassReader that is used to parse these bytes
    private val operationStats = if (collectStats && recordStats) OperationStats() else null

    @JvmOverloads
    fun addRef(delta: Int = 1) {
        refCount.addAndGet(delta)
    }

    @JvmOverloads
    fun release(delta: Int = 1) {
        check(refCount.addAndGet(-delta) >= 0) { "refCount underflow -- more release() calls than addRef()" }
    }

    internal fun tryReuse(): Boolean = refCount.compareAndSet(0, 1)

    internal fun reuse(size: Int) {
        operationStats?.flush(size)
        this.size = size
        readerCache = null
    }

    fun toByteArray(): ByteArray {
        reusableByteArrayStats?.converted?.add(size)
        return bytes.copyOf(size)
    }

    fun isNotEmpty(): Boolean = size > 0

    inline fun <T, reified R> traceOperation(name: String, readerFactory: (ReusableByteArray) -> R, operation: (R) -> T): T =
        traceOperation(name, R::class.java, readerFactory, operation)

    inline fun <T, R> traceOperation(name: String, readerClass: Class<R>, readerFactory: (ReusableByteArray) -> R, operation: (R) -> T): T {
        return traceOperation(nameWithSuffix(name, readerClass)) {
            val reader = reuseReader(readerClass) ?: readerFactory(this).also { saveReader(it) }
            operation(reader)
        }
    }

    inline fun <T> traceOperation(name: String, operation: () -> T): T {
        val start = startTrace()
        val result = operation()
        recordTrace(name, start)
        return result
    }

    @PublishedApi
    internal fun startTrace(): Long = if (collectStats) System.nanoTime() else 0L

    @PublishedApi
    internal fun recordTrace(name: String, start: Long) {
        if (collectStats) recordTrace(operationStats, name, size, System.nanoTime() - start)
    }

    @PublishedApi
    internal fun nameWithSuffix(name: String, readerClass: Class<*>): String =
        if (collectStats && readerCache?.javaClass != readerClass) "$name+" else name

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <R> reuseReader(readerClass: Class<R>): R? =
        if (readerCache?.javaClass == readerClass) readerCache as R else null

    @PublishedApi
    internal fun saveReader(reader: Any?) {
        readerCache = reader
    }
}

/**
 * Wraps byte array into fresh reusable byte array that is never reused,
 * so it is not necessarily to call [ReusableByteArray.release] on it afterwards.
 */
fun ByteArray.asReusableByteArray(): ReusableByteArray = ReusableByteArray(this)

// ---------------------------------------- ReusableByteArray cache ----------------------------------------

private const val MIN_SIZE = 64 // minimal allocation
private const val MAX_SIZE = 768 * 1024 // enough for the largest stdlib class
private const val KEEP_COUNT = 5 // one for compressed, one for decompressed, one cached, plus the max number of nested classes
private const val REUSE_ATTEMPTS = KEEP_COUNT - 1 // one is cached, others can be temporary retained when nested

private val reuse = ThreadLocal<Cache>()

private class Cache {
    val bufs = arrayOfNulls<ReusableByteArray>(KEEP_COUNT)
    var i = 0
}

/**
 * Returns reusable byte array with refCount = 1, call [ReusableByteArray.release] when done working with it.
 *
 * @param size requested [ReusableByteArray.size] of the result.
 * @param allocateSize the minimum size of the allocated array.
 */
fun takeReusableByteArrayRef(size: Int, allocateSize: Int = size): ReusableByteArray {
    require(size in 0..allocateSize) { "size=$size, allocateSize=$allocateSize" }
    val result = takeReusableByteArrayRefImpl(allocateSize)
    result.size = size
    return result
}

private fun takeReusableByteArrayRefImpl(size: Int): ReusableByteArray {
    if (size > MAX_SIZE) {
        reusableByteArrayStats?.tooBig?.add(size)
        return ReusableByteArray(size, recordStats = false)
    }
    val cache = reuse.get() ?: Cache().also { reuse.set(it) }
    var i: Int = -1
    var oldBuf: ReusableByteArray? = null
    for (attempt in 1..REUSE_ATTEMPTS) {
        i = cache.i
        cache.i = (i + 1) % KEEP_COUNT
        oldBuf = cache.bufs[i]
        if (oldBuf != null && oldBuf.bytes.size >= size) {
            if (oldBuf.tryReuse()) {
                reusableByteArrayStats?.reused?.add(size)
                oldBuf.reuse(size)
                return oldBuf
            } else {
                if (attempt == REUSE_ATTEMPTS) {
                    reusableByteArrayStats?.retained?.add(size)
                }
            }
        } else {
            reusableByteArrayStats?.resized?.add(size)
            break // always resize small ones, don't make another attempt
        }
    }
    val prevSize = oldBuf?.bytes?.size ?: (MIN_SIZE / 2)
    val buf = ReusableByteArray(size.coerceAtLeast(2 * prevSize).coerceAtMost(MAX_SIZE), recordStats = true)
    cache.bufs[i] = buf
    return buf
}

// ---------------------------------------- ReusableByteArray InputStream conversions ----------------------------------------

/**
 * Returns reusable byte array with refCount = 1, call [ReusableByteArray.release] when done working with it.
 */
fun InputStream.readToReusableByteArrayRef(fileLength: Long = -1): ReusableByteArray =
    use {
        check(fileLength in -1L..Int.MAX_VALUE) { "File length=$fileLength is invalid for reading into byte array" }
        readToReusableByteArrayRefImpl(fileLength.toInt())
    }

private fun InputStream.readToReusableByteArrayRefImpl(fileLength: Int): ReusableByteArray {
    if (this is ReusableByteArrayInputStream && isInitial()) {
        contentRef.addRef()
        return contentRef
    }
    var chunkSize = if (fileLength >= 0) fileLength else maxOf(available(), MIN_SIZE)
    var buf = takeReusableByteArrayRef(chunkSize)
    if (chunkSize == 0) return buf
    var start = buf.startTrace()
    var offset = 0
    while (true) {
        val n = read(buf.bytes, offset, chunkSize)
        if (n < chunkSize) {
            if (n > 0) offset += n
            buf.size = offset
            buf.recordTrace("InputStream.read", start)
            return buf
        }
        check(n == chunkSize)
        offset += n
        chunkSize = maxOf(available(), MIN_SIZE)
        val newSize = offset + chunkSize
        check(newSize >= 0) { "File is too big to read into byte array" }
        if (newSize > buf.bytes.size) {
            val buf2 = takeReusableByteArrayRef(newSize)
            buf.bytes.copyInto(buf2.bytes, endIndex = offset)
            buf.recordTrace("InputStream.read", start)
            buf.release()
            buf = buf2
            start = buf.startTrace()
        }
    }
}

/**
 * Wraps reusable byte array into an input stream without calling addRef, calling [InputStream.close] will release it.
 */
fun ReusableByteArray.asInputStream(): InputStream = ReusableByteArrayInputStream(this)

private class ReusableByteArrayInputStream(val contentRef: ReusableByteArray) : InputStream() {
    private var offset = 0
    private val size = contentRef.size

    fun isInitial(): Boolean = offset == 0

    override fun available(): Int {
        if (offset > size) throw IOException("closed")
        return size - offset
    }

    override fun read(): Int {
        if (offset > size) throw IOException("closed")
        if (offset == size) return -1
        return contentRef.bytes[offset++].toInt() and 0xff
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len < 0) throw IndexOutOfBoundsException()
        if (len == 0) return 0
        val n = minOf(len, size - offset)
        if (n < 0) throw IOException("closed")
        if (n == 0) return -1
        contentRef.bytes.copyInto(b, off, offset, offset + n)
        offset += n
        return n
    }

    override fun close() {
        if (offset > size) return // already closed
        offset = size + 1
        contentRef.release()
    }
}

// ---------------------------------------- Stats ----------------------------------------

private val reusableByteArrayStats: ReusableByteArrayStats? = if (collectStats) ReusableByteArrayStats() else null

fun dumpReusableByteArrayStats() {
    reusableByteArrayStats?.dump()
}

private open class CountSize {
    val count = AtomicInteger()
    val size = AtomicLong()

    fun add(size: Int) {
        count.incrementAndGet()
        this.size.addAndGet(size.toLong())
    }

    override fun toString(): String =
        "${count.toString().padStart(10)} times, " +
                "total size ${size.toString().padStart(12)} bytes"
}

private class CountSizeTime : CountSize() {
    val nanos = AtomicLong()

    fun add(size: Int, nanos: Long) {
        super.add(size)
        this.nanos.addAndGet(nanos)
    }

    override fun toString(): String {
        val ms = (nanos.get() / 1000000).toString().padStart(4, '0').padStart(6)
        val secIndex = ms.length - 3
        val sec = ms.substring(0, secIndex) + "." + ms.substring(secIndex)
        val kb = 1024L
        return "${count.toString().padStart(10)} times, " +
                "total time $sec sec, " +
                "average time ${(nanos.get() * kb / size.get().coerceAtLeast(kb)).toString().padStart(6)} ns per KiB, " +
                "average size ${(size.get() / count.get()).toString().padStart(6)} bytes"
    }
}

private class ReusableByteArrayStats {
    val reused = CountSize()
    val retained = CountSize()
    val resized = CountSize()
    val tooBig = CountSize()
    val converted = CountSize()

    val times = ConcurrentHashMap<String, CountSizeTime>()
    val groups = ConcurrentHashMap<List<String>, CountSizeTime>()

    fun dump() {
        println("=== ReusableByteArrayStats ===")
        println("  --- Allocations ---")
        println("    Reused    $reused")
        println("    Retained  $retained")
        println("    Resized   $resized")
        println("    Too big   $tooBig")
        println("    Converted $converted")
        println("  --- Operations ---")
        val nameTimes = times.toList().sortedBy { it.first }
        val maxNameLen = nameTimes.maxOf { it.first.length }
        for ((name, times) in nameTimes) {
            println("    ${name.padEnd(maxNameLen)} $times")
        }
        println("  --- Operation groups ---")
        val groupTimes = groups.toList().sortedByDescending { it.second.count.get() }
        val maxGroupLen = groupTimes.maxOf { it.first.toString().length }
        for ((group, times) in groupTimes) {
            println("    ${group.toString().padEnd(maxGroupLen)} $times")
        }
        println("  ## Legend: '+' suffix means that operation had created a fresh ClassReader")
        println("  ##         '*' suffix means that operation was performed multiple times")
    }
}

private fun recordTrace(operationStats: OperationStats?, name: String, size: Int, nanos: Long) {
    val stats = reusableByteArrayStats!!
    val times = stats.times.computeIfAbsent(name) { CountSizeTime() }
    times.add(size, nanos)
    operationStats?.run {
        val prev = ops.lastOrNull()
        when {
            prev != null && prev.endsWith("*") && prev.dropLast(1) == name -> {} // already listed as multiple
            prev == name -> ops[ops.lastIndex] = "$name*" // replace last with multiple
            else -> ops += name // add name to list
        }
        sumNanos += nanos
    }
}

private class OperationStats {
    val ops = ArrayList<String>()
    var sumNanos = 0L

    fun flush(size: Int) {
        val stats = reusableByteArrayStats!!
        val times = stats.groups[ops] ?: run {
            val key = ops.toList()
            stats.groups.computeIfAbsent(key) { CountSizeTime() }
        }
        times.add(size, sumNanos)
        ops.clear()
        sumNanos = 0
    }
}