/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.reflect.KClass

/**
 * [ComponentArrayOwner] based on [ArrayMap] with flexible size and should be used for
 *   storing services in entities with limited number of instances, like FirSession
 */
abstract class ComponentArrayOwner<K : Any, V : Any> : AbstractArrayMapOwner<K, V>() {
    final override val arrayMap: ArrayMap<V> =
        ArrayMapImpl()

    final override fun registerComponent(tClass: KClass<out K>, value: V) {
        arrayMap[typeRegistry.getId(tClass)] = value
    }

    protected operator fun get(key: KClass<out K>): V {
        val id = typeRegistry.getId(key)
        return arrayMap[id] ?: error("No '$key'($id) component in array: $this")
    }
}