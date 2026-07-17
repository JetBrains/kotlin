/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.declarations.DelicateIrParameterIndexSetter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> lazyVar(lock: IrLock, initializer: () -> T): ReadWriteProperty<Any?, T> = SynchronizedLazyVar(lock, initializer)

fun lazyVarForParameters(lock: IrLock, initializer: () -> List<IrValueParameter>): ReadWriteProperty<Any?, List<IrValueParameter>> =
    SynchronizedLazyVarForParameters(lock, initializer)

private open class SynchronizedLazyVar<T>(val lock: IrLock, initializer: () -> T) : ReadWriteProperty<Any?, T> {
    @Volatile
    private var isInitialized = false

    private var initializer: (() -> T)? = initializer

    @Volatile
    private var _value: Any? = null

    private val value: T
        get() {
            @Suppress("UNCHECKED_CAST")
            if (isInitialized) return _value as T
            synchronized(lock) {
                if (!isInitialized) {
                    _value = initializer!!()
                    isInitialized = true
                    initializer = null
                }
                @Suppress("UNCHECKED_CAST")
                return _value as T
            }
        }

    override fun toString(): String = if (isInitialized) value.toString() else "Lazy value not initialized yet."

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(lock) {
            this._value = value
            isInitialized = true
        }
    }
}

private class SynchronizedLazyVarForParameters(
    lock: IrLock, initializer: () -> List<IrValueParameter>
) : SynchronizedLazyVar<List<IrValueParameter>>(lock, initializer) {
    @OptIn(DelicateIrParameterIndexSetter::class)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<IrValueParameter>) {
        super.setValue(thisRef, property, value)
        for (parameter in value) {
            parameter.indexInParameters = -1
        }
        for ([index, parameter] in value.withIndex()) {
            parameter.indexInParameters = index
        }
    }
}
