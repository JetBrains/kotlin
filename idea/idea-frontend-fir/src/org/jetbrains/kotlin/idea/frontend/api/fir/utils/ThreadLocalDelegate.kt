/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import kotlin.reflect.KProperty


internal class ThreadLocalValue<V>(private val threadLocal: ThreadLocal<V>) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): V = threadLocal.get()
}

internal inline fun <T> threadLocal(crossinline init: () -> T): ThreadLocalValue<T> =
    ThreadLocalValue(ThreadLocal.withInitial { init() })
