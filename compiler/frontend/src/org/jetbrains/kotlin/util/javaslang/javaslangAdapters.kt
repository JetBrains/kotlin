/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.util.javaslang

import javaslang.Tuple2
import javaslang.control.Option

typealias ImmutableMap<K, V> = javaslang.collection.Map<K, V>
typealias ImmutableHashMap<K, V> = javaslang.collection.HashMap<K, V>
typealias ImmutableSet<E> = javaslang.collection.Set<E>
typealias ImmutableHashSet<E> = javaslang.collection.HashSet<E>
typealias ImmutableLinkedHashSet<E> = javaslang.collection.LinkedHashSet<E>

operator fun <T> Tuple2<T, *>.component1(): T = _1()
operator fun <T> Tuple2<*, T>.component2(): T = _2()

fun <T> Option<T>.getOrNull(): T? = getOrElse(null as T?)
fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = get(k)?.getOrElse(null as V?)
