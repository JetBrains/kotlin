/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.storage

import kotlin.reflect.KProperty

interface MemoizedFunctionToNotNull<in P, out R : Any> : Function1<P, R> {
    fun isComputed(key: P): Boolean
}

interface MemoizedFunctionToNullable<in P, out R : Any> : Function1<P, R?> {
    fun isComputed(key: P): Boolean
}

interface NotNullLazyValue<out T : Any> : Function0<T> {
    fun isComputed(): Boolean
    fun isComputing(): Boolean
}

interface NullableLazyValue<out T : Any> : Function0<T?> {
    fun isComputed(): Boolean
    fun isComputing(): Boolean
}

operator fun <T : Any> NotNullLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T = invoke()

operator fun <T : Any> NullableLazyValue<T>.getValue(_this: Any?, p: KProperty<*>): T? = invoke()

interface CacheWithNullableValues<in K, V : Any> {
    fun computeIfAbsent(key: K, computation: () -> V?): V?
}

interface CacheWithNotNullValues<in K, V : Any> {
    fun computeIfAbsent(key: K, computation: () -> V): V
}
