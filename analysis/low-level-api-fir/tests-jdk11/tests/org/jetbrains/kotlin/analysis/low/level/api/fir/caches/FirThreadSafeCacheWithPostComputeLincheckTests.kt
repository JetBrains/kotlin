/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.FirThreadSafeCachesFactory
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlinx.lincheck.RandomProvider
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.paramgen.ParameterGenerator
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.concurrent.locks.ReentrantLock

@Param(name = "context", gen = ContextGenerator::class)
class SingleFirThreadSafeCacheWithPostComputeLincheckTest {
    private val cache: FirCache<Int, Value, Context?> = FirThreadSafeCachesFactory.createCacheWithPostCompute(
        { key, context -> Value(key * 2) to context?.x },
        { key, value, contextValue -> value.x += 1 + (contextValue ?: 0) }
    )

    @Operation
    fun get(
        key: Int,
        @Param(name = "context") context: Context?,
    ): Value = cache.getValue(key, context)

    @Test
    fun modelCheckingTest() = ModelCheckingOptions().check(this::class)

    @Test
    fun stressTest() = StressOptions().check(this::class)
}

/**
 * This test reproduces the scenario encountered in [KT-70327](https://youtrack.jetbrains.com/issue/KT-70327) with a very tight key
 * distribution (only 1 and 2) and a recursive cache access of the other key during post-computation.
 *
 * In other words, computation of `get(1)` calls `get(2)` during post-computation, and `get(2)` calls `get(1)`. With the correct thread
 * switching behavior (each thread acquires exactly one lock), which Lincheck generates deterministically for us, we achieve a simultaneous
 * post-computation of both values and hence the deadlock described in [KT-70327](https://youtrack.jetbrains.com/issue/KT-70327).
 *
 * The computed results differ based on whether `get(1)` or `get(2)` is computed first. This is not a problem, however, because we can
 * express this difference sequentially. The Lincheck test simply checks if the concurrent result could be expressed using any valid
 * sequential order.
 */
@Param(name = "key", gen = IntGen::class, conf = "1:2")
class RecursiveSingleFirThreadSafeCacheWithPostComputeLincheckTest {
    private val cache: FirCache<Int, Value, Any?> = FirThreadSafeCachesFactory.createCacheWithPostCompute(
        { key, context -> Value(key) to null },
        { key, value, _ ->
            // Infinite recursion is avoided by the following: Post-computation of `get(otherKey)` will lead to another `get(key)` call. At
            // this point, the cache already contains the value for `get(key)`, albeit in a "post-computing" state. However, since we're in
            // the same thread as the running post-computation, the cache returns this incomplete value.
            //
            // We cannot use the context to control recursion (e.g. `context != null) since the context is captured inside the
            // `ValueWithPostCompute`'s `calculate` lambda. So the first call to `get` captures the context, making the order of calls
            // important. A first call `get(1, null)` via the post-computation would capture a `null` context while a first call
            // `get(1, Context(0))` would capture that context. Crucially, if the first call is `get(1, Context(0))`, then the
            // post-computation's `get(1, null)` call would still recurse, as the underlying `ValueWithPostCompute` would have a non-null
            // context.
            val otherKey = if (key == 1) 2 else 1
            val extra = cache.getValue(otherKey, null).x

            value.x += 1 + extra
        },
        sharedComputationLock = ReentrantLock(),
    )

    @Operation
    fun get(
        @Param(name = "key") key: Int,
    ): Value = cache.getValue(key, null)

    @Test
    fun modelCheckingTest() = ModelCheckingOptions()
        // Don't generate any operations before concurrent execution. The default value is 5, which would generate five `get(1)` and
        // `get(2)` calls. After these calls, the cache would already be fully populated, and post-computation would never have happened
        // during concurrent execution.
        .actorsBefore(0)
        .check(this::class)
}

/**
 * A slight variation of [RecursiveSingleFirThreadSafeCacheWithPostComputeLincheckTest] with two caches, each accessing the other's
 * post-computing value. In real lazy resolution, the deadlock described in [KT-70327](https://youtrack.jetbrains.com/issue/KT-70327) may
 * happen across two different `JavaSymbolProvider`s, for example when two Java classes from different modules reference each other. It can
 * only happen with recursive module dependencies, but these are possible at least in the JPS build system.
 *
 * Since the deadlock may appear across different caches, solutions which lock post-computations into a single thread per cache aren't
 * sufficient, and the test makes sure that we don't build such a solution.
 */
class RecursiveMultiFirThreadSafeCacheWithPostComputeLincheckTest {
    private val sharedComputationLock = ReentrantLock()

    private val cache1: FirCache<Int, Value, Any?> = createCache { cache2 }

    private val cache2: FirCache<Int, Value, Any?> = createCache { cache1 }

    private inline fun createCache(crossinline getOtherCache: () -> FirCache<Int, Value, Any?>): FirCache<Int, Value, Any?> =
        FirThreadSafeCachesFactory.createCacheWithPostCompute(
            { key, context -> Value(key) to null },
            { key, value, hasContext ->
                val extra = getOtherCache().getValue(key, null).x
                value.x += 1 + extra
            },
            sharedComputationLock,
        )

    @Operation
    fun getFromCache1(): Value = cache1.getValue(1, null)

    @Operation
    fun getFromCache2(): Value = cache2.getValue(1, null)

    @Test
    fun modelCheckingTest() = ModelCheckingOptions().actorsBefore(0).check(this::class)
}

data class Value(var x: Int) : Serializable

data class Context(val x: Int) : Serializable

class ContextGenerator(randomProvider: RandomProvider, configuration: String) : ParameterGenerator<Context> {
    private val intGenerator: IntGen = IntGen(randomProvider, "0:10")

    override fun generate(): Context = Context(intGenerator.generate())
}
