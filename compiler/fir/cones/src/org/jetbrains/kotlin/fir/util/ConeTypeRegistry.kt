/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.util

import org.jetbrains.kotlin.util.TypeRegistry
import java.util.concurrent.ConcurrentHashMap

abstract class ConeTypeRegistry<K : Any, V : Any> : TypeRegistry<K, V>() {
    override fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int {
        return this.computeIfAbsent(key, compute)
    }
}
