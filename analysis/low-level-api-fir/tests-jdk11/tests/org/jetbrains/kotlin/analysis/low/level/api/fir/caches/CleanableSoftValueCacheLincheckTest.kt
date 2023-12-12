/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.forClasses
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.jupiter.api.Test

/**
 * Verifies the linearizability of [CleanableSoftValueCache].
 *
 * This test cannot rely on garbage collector and soft reference queue semantics, so we associate no cleaning operations with it.
 *
 * Various functions of the cache are not checked by Lincheck:
 *
 * - [CleanableSoftValueCache.clear] is not checked because it must be executed in a write action, which guarantees single-threadedness.
 * - [CleanableSoftValueCache.size] and [CleanableSoftValueCache.isEmpty] are not checked because the underlying `ConcurrentHashMap`'s
 *   implementation of these properties isn't guaranteed to immediately take effect after map operations (see the `ConcurrentHashMap`
 *   section in the book "Java Concurrency in Practice").
 * - [CleanableSoftValueCache.keys] is not checked for linearizability because it is only weakly consistent via the underlying concurrent
 *   hash map implementation.
 */
class CleanableSoftValueCacheLincheckTest {
    private val cache = CleanableSoftValueCache<Int, Int> { SoftValueCleaner { } }

    @Operation
    fun get(key: Int): Int? = cache.get(key)

    @Operation
    fun computeIfAbsent(key: Int, value: Int): Int = cache.computeIfAbsent(key) { value }

    @Operation
    fun compute(key: Int, offset: Int): Int? = cache.compute(key) { _, base -> (base ?: 0) + offset }

    @Operation
    fun put(key: Int, value: Int): Int? = cache.put(key, value)

    @Operation
    fun remove(key: Int): Int? = cache.remove(key)

    /**
     * The guarantee for [ConcurrentHashMap][java.util.concurrent.ConcurrentHashMap] is required for model checking to succeed because
     * `ConcurrentHashMap` doesn't pass Lincheck model checking itself.
     */
    @Test
    fun modelCheckingTest() = ModelCheckingOptions()
        .addGuarantee(forClasses("java.util.concurrent.ConcurrentHashMap").allMethods().treatAsAtomic())
        .check(this::class)

    @Test
    fun stressTest() = StressOptions().check(this::class)
}
