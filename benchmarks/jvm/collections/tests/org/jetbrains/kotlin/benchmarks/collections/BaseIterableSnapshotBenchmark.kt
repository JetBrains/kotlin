/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.collections

import kotlinx.benchmark.*

@State(Scope.Benchmark)
abstract class BaseIterableSnapshotBenchmark {
    abstract var sourceKind: String

    @Param("0", "1", "2", "10", "100", "1000", "10000", "100000")
    var size: Int = 0

    protected lateinit var collection: Collection<String>

    @Setup
    fun setUp() {
        collection = when (sourceKind) {
            "ArrayList" -> sizedArrayList(size)
            "ListWorstCase" -> object : List<String> by sizedArrayList(size) {
                override fun iterator(): Iterator<String> {
                    val iterator = listIterator()
                    return object : Iterator<String> {
                        override fun hasNext(): Boolean = iterator.hasNext()
                        override fun next(): String = iterator.next()
                    }
                }
            }
            "NoSuchElementThrowingList" -> {
                // Used to provide a lying iterator that will throw `NoSuchElementException` instead of returning the last element, hitting
                // the exception handling path in the Kotlin/Native `Iterable<T>.toList() implementation.
                // Note that this effectively reduces the benchmarks with size of 0 and 1 to `ListWorstCase`.
                val backing = sizedArrayList((size - 1).coerceAtLeast(0))

                object : List<String> by sizedArrayList(size) {
                    override fun iterator(): Iterator<String> {
                        val iterator = backing.listIterator()

                        return object : Iterator<String> {
                            override fun hasNext(): Boolean = iterator.hasNext()
                            override fun next(): String = iterator.next()
                        }
                    }
                }
            }
            "HashSet" -> HashSet(sizedArrayList(size))
            else -> throw IllegalStateException("Unknown source kind: $sourceKind")
        }
    }

    @Benchmark
    fun toList(blackhole: Blackhole) {
        blackhole.consume((collection as Iterable<String>).toList())
    }

    private fun sizedArrayList(count: Int) = ArrayList<String>().apply {
        repeat(count) { index ->
            add(index.toString())
        }
    }
}

@State(Scope.Benchmark)
abstract class BaseSingletonListSnapshotBenchmark {
    protected lateinit var collection: Collection<String>

    @Setup
    fun setUp() {
        collection = listOf("single element")
    }

    @Benchmark
    fun toList(blackhole: Blackhole) {
        blackhole.consume((collection as Iterable<String>).toList())
    }
}
