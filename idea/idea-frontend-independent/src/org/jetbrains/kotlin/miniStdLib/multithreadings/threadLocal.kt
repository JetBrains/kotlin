/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.miniStdLib.multithreadings

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@JvmInline
value class JavaThreadLocalDelegate<V> internal constructor(private val threadLocal: ThreadLocal<V>):  ReadWriteProperty<Any?, V> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return threadLocal.get()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        threadLocal.set(value)
    }

}

fun <T> javaThreadLocal(initialValue: () -> T): JavaThreadLocalDelegate<T> =
    JavaThreadLocalDelegate(ThreadLocal.withInitial(initialValue))

fun <T> javaThreadLocal(initialValue: T): JavaThreadLocalDelegate<T> =
    JavaThreadLocalDelegate(ThreadLocal.withInitial { initialValue })