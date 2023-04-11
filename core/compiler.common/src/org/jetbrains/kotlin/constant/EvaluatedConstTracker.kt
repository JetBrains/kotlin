/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.constant

import java.util.concurrent.ConcurrentHashMap

abstract class EvaluatedConstTracker {
    abstract fun save(start: Int, end: Int, file: String, constant: ConstantValue<*>)
    abstract fun load(start: Int, end: Int, file: String): ConstantValue<*>?
    abstract fun load(file: String): Map<Pair<Int, Int>, ConstantValue<*>>?

    companion object {
        /**
         * Right now there are two places where we want to create this tracker.
         * 1. Right before `fir2ir` phase. We need to store evaluated values to use them later in const value serialization.
         * 2. In tests for K1 IR. This is needed ONLY for tests to log results of interpretation on lowering level.
         */
        fun create(): EvaluatedConstTracker {
            return DefaultEvaluatedConstTracker()
        }
    }
}

private class DefaultEvaluatedConstTracker : EvaluatedConstTracker() {
    private val storage = ConcurrentHashMap<String, ConcurrentHashMap<Pair<Int, Int>, ConstantValue<*>>>()

    override fun save(start: Int, end: Int, file: String, constant: ConstantValue<*>) {
        storage
            .getOrPut(file) { ConcurrentHashMap() }
            .let { it[start to end] = constant }
    }

    override fun load(start: Int, end: Int, file: String): ConstantValue<*>? {
        return storage[file]?.get(start to end)
    }

    override fun load(file: String): Map<Pair<Int, Int>, ConstantValue<*>>? {
        return storage[file]
    }
}

