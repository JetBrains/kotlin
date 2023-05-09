/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> threadLocal(initializer: () -> T): ReadWriteProperty<Any?, T> = ThreadLocalDelegate(initializer)

private class ThreadLocalDelegate<T>(private val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private val map = ConcurrentHashMap<Thread, T>()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return map.getOrPut(Thread.currentThread()) {
            initializer()
        }
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        map[Thread.currentThread()] = value
    }

    override fun toString(): String =
        "ThreadLocalDelegate(${map.entries.joinToString { "#${it.key.id}=>${it.value}" }})"
}