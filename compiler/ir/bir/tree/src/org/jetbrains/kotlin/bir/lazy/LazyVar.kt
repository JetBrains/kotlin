/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.lazy

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.acceptLite
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SynchronizedLazyBirElementVar<P : BirLazyElementBase, T>(
    private val parent: BirLazyElementBase,
    initializer: P.() -> T,
) : ReadWriteProperty<Any?, T> {
    private var initializer: (P.() -> T)? = initializer

    @Volatile
    private var _value: Any? = NotInitializedValue

    val isInitialized
        get() = _value !== NotInitializedValue

    val value: T
        get() {
            @Suppress("UNCHECKED_CAST")
            if (isInitialized) return _value as T
            synchronized(this) {
                if (!isInitialized) {
                    @Suppress("UNCHECKED_CAST")
                    _value = initializer!!(parent as P)
                    initializer = null
                }
                @Suppress("UNCHECKED_CAST")
                return _value as T
            }
        }

    override fun toString(): String = if (isInitialized) value.toString() else "Lazy value not initialized yet."

    @Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
    override inline fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    @Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
    override inline fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        BirLazyElementBase.mutationNotSupported()
    }

    private object NotInitializedValue
}

internal fun <T : BirElement?> SynchronizedLazyBirElementVar<*, T>.acceptLiteIfPresent(visitor: BirElementVisitorLite) {
    if (isInitialized) {
        value?.acceptLite(visitor)
    }
}