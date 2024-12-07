/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util.vavr

import io.vavr.Tuple2

typealias ImmutableMap<K, V> = io.vavr.collection.Map<K, V>
typealias ImmutableHashMap<K, V> = io.vavr.collection.HashMap<K, V>
typealias ImmutableSet<E> = io.vavr.collection.Set<E>
typealias ImmutableHashSet<E> = io.vavr.collection.HashSet<E>
typealias ImmutableLinkedHashSet<E> = io.vavr.collection.LinkedHashSet<E>

operator fun <T> Tuple2<T, *>.component1(): T = _1()
operator fun <T> Tuple2<*, T>.component2(): T = _2()

fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = get(k)?.getOrElse(null as V?)
