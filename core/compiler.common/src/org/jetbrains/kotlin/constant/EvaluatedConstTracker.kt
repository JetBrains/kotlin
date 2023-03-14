/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.constant

import java.util.concurrent.ConcurrentHashMap

abstract class EvaluatedConstTracker {
    abstract fun save(start: Int, end: Int, constant: ConstantValue<*>)
    abstract fun load(start: Int, end: Int): ConstantValue<*>?
}

class DefaultEvaluatedConstTracker : EvaluatedConstTracker() {
    private val storage = ConcurrentHashMap<Pair<Int, Int>, ConstantValue<*>>()

    override fun save(start: Int, end: Int, constant: ConstantValue<*>) {
        storage[start to end] = constant
    }

    override fun load(start: Int, end: Int): ConstantValue<*>? {
        return storage[start to end]
    }
}

