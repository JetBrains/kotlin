/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.constant

import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

abstract class EvaluatedConstTracker {
    abstract fun save(start: Int, end: Int, fileKey: Key?, constant: ConstantValue<*>)
    abstract fun load(start: Int, end: Int, fileKey: Key?): ConstantValue<*>?

    @TestOnly
    abstract fun loadAllForTests(fileKey: Key?): Map<Pair<Int, Int>, ConstantValue<*>>?

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

    interface Key {
        data class StringBased(val value: String) : Key {
            override fun asStringBasedKey(): StringBased = this
        }

        fun asStringBasedKey(): StringBased
    }
}

private class DefaultEvaluatedConstTracker : EvaluatedConstTracker() {
    private val storage = ConcurrentHashMap<Key, ConcurrentHashMap<Pair<Int, Int>, ConstantValue<*>>>()

    override fun save(start: Int, end: Int, fileKey: Key?, constant: ConstantValue<*>) {
        if (fileKey == null) return
        storage
            .getOrPut(fileKey) { ConcurrentHashMap() }
            .let { it[start to end] = constant }
    }

    override fun load(start: Int, end: Int, fileKey: Key?): ConstantValue<*>? {
        if (fileKey == null) return null
        return storage[fileKey]?.get(start to end)
    }

    /**
     * Saving the constant evaluation information by this key is needed for two purposes:
     * 1. Main one: the metadata serializer extract the results of the constant evaluation which happened on the IR level using
     *    only FIR entities. So for this scenario the `FirFileSymbol` is used as a key.
     * 2. Secondary one: in tests for constant evaluation we want to extract all evaluated constants on the second (klib) phase\
     *    using fir-based IR files (for some reason). To support this case, there is a fallback with a String (file path) key,
     *    which could be computed both from FirFile and IrFile.
     */
    override fun loadAllForTests(fileKey: Key?): Map<Pair<Int, Int>, ConstantValue<*>>? {
        if (fileKey == null) return null
        return storage[fileKey].orEmpty() + storage[fileKey.asStringBasedKey()].orEmpty()
    }
}

