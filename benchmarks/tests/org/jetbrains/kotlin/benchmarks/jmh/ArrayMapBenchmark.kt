/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Compares construction and access performance of the sparse [SparseArrayMap] (a faithful copy of the production
 * `ArrayMapImpl`) against the proposed [TinyArrayMap] for small maps of 2-4 entries. See KT-87817.
 *
 * The two profiles model the key characteristic of `ArrayMapImpl`: its backing array is sized by the *maximum* key
 * index (grown from a base of 20 by doubling), not by the number of entries. Each iteration draws a fresh set of
 * `count` **distinct random** indices from the profile's range:
 *
 *  - `tight` (range 0..7): every index fits in the base 20-element array.
 *  - `wide`  (range 0..63): the max index varies per iteration, so the sparse array lands on 20, 40, or 80 elements.
 *
 * Randomizing within a fixed range (rather than a monotonic sequence like 4, 23, 32, 51) keeps the max-index
 * distribution — and thus the sparse growth cost — representative across counts, so count=4 is no longer forced into
 * the worst case. The random set is regenerated per iteration in [setUpIteration] (never on the hot path), and the
 * shared [rng] is seeded once per trial in [setUpTrial], so successive iterations sample different distributions while
 * the whole run stays reproducible. Use a higher `-Piterations=N` to average over more distributions.
 *
 * [TinyArrayMap] is insensitive to index magnitude, so the `tight`/`wide` split should move only the sparse numbers.
 *
 * Run e.g.:
 *   ./gradlew :benchmarks:testBenchmark -Pinclude="ArrayMapBenchmark" -Piterations=25 -q
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
open class ArrayMapBenchmark {

    @Param("2", "3", "4")
    private var count: Int = 0

    @Param("tight", "wide")
    private var profile: String = ""

    private var rangeSize: Int = 0
    private lateinit var rng: Random

    /** Distinct present indices for the current iteration; size [count]. */
    private lateinit var indices: IntArray

    /** A guaranteed-absent (but in-range) index for the current iteration's miss path. */
    private var missIndex: Int = -1

    /** Shared boxed values, one per present key (content is irrelevant to the measurement). */
    private lateinit var values: Array<Any?>

    /** Pre-built maps for the read-path benchmarks. */
    private lateinit var sparse: SparseArrayMap
    private lateinit var tiny: TinyArrayMap

    @Setup(Level.Trial)
    fun setUpTrial() {
        rangeSize = when (profile) {
            "tight" -> 8   // indices 0..7
            "wide" -> 64   // indices 0..63
            else -> error("Unknown profile: $profile")
        }
        rng = Random(seed = 0x5EEDL)
        values = Array(count) { Any() }
    }

    @Setup(Level.Iteration)
    fun setUpIteration() {
        // Sample count + 1 distinct indices: the first `count` are present, the extra is an in-range guaranteed miss.
        val sample = sampleDistinct(rangeSize, count + 1, rng)
        indices = sample.copyOf(count)
        missIndex = sample[count]

        sparse = SparseArrayMap().also { map -> for (k in 0 until count) map[indices[k]] = values[k] }
        tiny = TinyArrayMap().also { map -> for (k in 0 until count) map[indices[k]] = values[k] }
    }

    /** Partial Fisher-Yates: returns [n] distinct indices from `0 until rangeSize`. Requires `n <= rangeSize`. */
    private fun sampleDistinct(rangeSize: Int, n: Int, rng: Random): IntArray {
        val pool = IntArray(rangeSize) { it }
        for (i in 0 until n) {
            val j = i + rng.nextInt(rangeSize - i)
            val tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp
        }
        return pool.copyOf(n)
    }

    // --- Construction (in-place set, mirrors ArrayMapImpl.set) ---

    @Benchmark
    fun constructSparse(bh: Blackhole) {
        val map = SparseArrayMap()
        for (k in 0 until count) map[indices[k]] = values[k]
        bh.consume(map)
    }

    @Benchmark
    fun constructTiny(bh: Blackhole) {
        val map = TinyArrayMap()
        for (k in 0 until count) map[indices[k]] = values[k]
        bh.consume(map)
    }

    /**
     * Construction via the thread-safe copy-on-write strategy proposed for [TinyArrayMap]: every added entry publishes
     * a fresh instance. This trades allocation churn for lock-free reads; measured separately from the in-place path.
     */
    @Benchmark
    fun constructTinyCopyOnWrite(bh: Blackhole) {
        var map = TinyArrayMap()
        for (k in 0 until count) map = map.withEntry(indices[k], values[k])
        bh.consume(map)
    }

    // --- Access: hit (read every present key) ---

    @Benchmark
    fun getSparseHit(bh: Blackhole) {
        for (k in 0 until count) bh.consume(sparse[indices[k]])
    }

    @Benchmark
    fun getTinyHit(bh: Blackhole) {
        for (k in 0 until count) bh.consume(tiny[indices[k]])
    }

    // --- Access: miss (worst case: full scan / bounds check, no match) ---

    @Benchmark
    fun getSparseMiss(bh: Blackhole) {
        bh.consume(sparse[missIndex])
    }

    @Benchmark
    fun getTinyMiss(bh: Blackhole) {
        bh.consume(tiny[missIndex])
    }

    // --- Size: TinyArrayMap has no stored counter, so it is computed from the index fields ---

    /** Baseline: SparseArrayMap keeps a maintained counter, so this is a plain field read (O(1)). */
    @Benchmark
    fun sizeSparse(bh: Blackhole) {
        bh.consume(sparse.size)
    }

    @Benchmark
    fun sizeTinyTrailing(bh: Blackhole) {
        bh.consume(tiny.sizeByTrailing())
    }

    @Benchmark
    fun sizeTinyCounting(bh: Blackhole) {
        bh.consume(tiny.sizeByCounting())
    }
}

/**
 * A faithful, self-contained copy of the production `ArrayMapImpl` strategy: a direct-index sparse array grown from a
 * base of [DEFAULT_SIZE] by [INCREASE_K]. `internal` visibility prevents using the real class from another module, so
 * the essential characteristics are replicated here.
 */
private class SparseArrayMap {
    private var data: Array<Any?> = arrayOfNulls(DEFAULT_SIZE)

    var size: Int = 0
        private set

    private fun ensureCapacity(index: Int) {
        if (data.size > index) return
        var newSize = data.size
        do {
            newSize *= INCREASE_K
        } while (newSize <= index)
        data = data.copyOf(newSize)
    }

    operator fun set(index: Int, value: Any?) {
        ensureCapacity(index)
        if (data[index] == null) size++
        data[index] = value
    }

    operator fun get(index: Int): Any? = data.getOrNull(index)

    companion object {
        private const val DEFAULT_SIZE = 20
        private const val INCREASE_K = 2
    }
}

/**
 * The proposed replacement for small maps (KT-87817): four inlined `index`/`value` field pairs, with unoccupied slots
 * sentineled to -1. Since `TypeRegistry` ids are always >= 0, an empty slot can never match a queried id, so `get`
 * needs no occupancy check beyond the field comparisons. ~48 bytes, single object, no backing array.
 *
 * [set] mutates in place (mirroring `ArrayMapImpl.set`); [withEntry] provides the copy-on-write step used by the
 * thread-safe variant.
 */
private class TinyArrayMap {
    private var index0: Int = -1
    private var value0: Any? = null
    private var index1: Int = -1
    private var value1: Any? = null
    private var index2: Int = -1
    private var value2: Any? = null
    private var index3: Int = -1
    private var value3: Any? = null

    operator fun set(index: Int, value: Any?) {
        when {
            index0 == index || index0 == -1 -> {
                index0 = index; value0 = value
            }
            index1 == index || index1 == -1 -> {
                index1 = index; value1 = value
            }
            index2 == index || index2 == -1 -> {
                index2 = index; value2 = value
            }
            index3 == index || index3 == -1 -> {
                index3 = index; value3 = value
            }
            else -> error("TinyArrayMap overflow: no free slot for index $index")
        }
    }

    operator fun get(index: Int): Any? {
        if (index == index0) return value0
        if (index == index1) return value1
        if (index == index2) return value2
        if (index == index3) return value3
        return null
    }

    /**
     * Size via the highest occupied slot: since [set] fills slots front-to-back with no gaps, the topmost non-empty
     * index determines the size. Early-returns, so it does 1 comparison for a full map and 4 for an empty one.
     */
    fun sizeByTrailing(): Int = when {
        index3 != -1 -> 4
        index2 != -1 -> 3
        index1 != -1 -> 2
        index0 != -1 -> 1
        else -> 0
    }

    /** Size by counting every occupied slot: always 4 comparisons, but tolerant of gaps. */
    fun sizeByCounting(): Int {
        var size = 0
        if (index0 != -1) size += 1
        if (index1 != -1) size += 1
        if (index2 != -1) size += 1
        if (index3 != -1) size += 1
        return size
    }

    /** Returns a copy with ([index], [value]) added/overwritten — the copy-on-write step for thread-safe writes. */
    fun withEntry(index: Int, value: Any?): TinyArrayMap {
        val copy = TinyArrayMap()
        copy.index0 = index0; copy.value0 = value0
        copy.index1 = index1; copy.value1 = value1
        copy.index2 = index2; copy.value2 = value2
        copy.index3 = index3; copy.value3 = value3
        copy[index] = value
        return copy
    }
}
