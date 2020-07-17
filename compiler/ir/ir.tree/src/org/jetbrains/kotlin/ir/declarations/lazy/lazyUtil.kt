/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.ir.declarations.withInitialIr
import kotlin.reflect.KProperty

fun <T> lazyVar(initializer: () -> T): UnsafeLazyVar<T> = UnsafeLazyVar(initializer)

class UnsafeLazyVar<T>(initializer: () -> T) {
    private var isInitialized = false;
    private var initializer: (() -> T)? = initializer
    private var _value: Any? = null

    private val value: T
        get() {
            if (!isInitialized) {
                withInitialIr { _value = initializer!!() }
                isInitialized = true
                initializer = null
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun toString(): String = if (isInitialized) value.toString() else "Lazy value not initialized yet."

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this._value = value
        isInitialized = true
    }
}