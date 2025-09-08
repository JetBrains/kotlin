/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.utils

import org.ehcache.sizeof.SizeOf
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [LLObjectSizeCalculator] should only be used for statistics gathering. It should only be used during development, as usage of `SizeOf`
 * attaches a dynamic agent, which is undesirable for production usage.
 */
internal object LLObjectSizeCalculator {
    private val cache = ConcurrentHashMap<KClass<*>, Int>()

    fun shallowSize(value: Any): Int =
        cache.computeIfAbsent(value::class) { kClass ->
            val javaClass = kClass.java
            if (javaClass.isArray || javaClass.isPrimitive) {
                error("Shallow size should not be naively calculated for arrays or primitives: '$value'.")
            }

            SizeOf.newInstance().sizeOf(value).toInt()
        }
}
