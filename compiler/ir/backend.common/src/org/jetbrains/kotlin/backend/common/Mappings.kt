/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.*
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

interface Mapping {
    val defaultArgumentsDispatchFunction: Delegate<IrFunction, IrFunction>
    val defaultArgumentsOriginalFunction: Delegate<IrFunction, IrFunction>
    val suspendFunctionToCoroutineConstructor: Delegate<IrFunction, IrConstructor>
    val lateInitFieldToNullableField: Delegate<IrField, IrField>
    val inlineClassMemberToStatic: Delegate<IrFunction, IrSimpleFunction>
    val capturedFields: Delegate<IrClass, Collection<IrField>>
    val capturedConstructors: Delegate<IrConstructor, IrConstructor>
    val reflectedNameAccessor: Delegate<IrClass, IrSimpleFunction>

    abstract class Delegate<K : IrDeclaration, V> {
        abstract operator fun get(key: K): V?

        abstract operator fun set(key: K, value: V?)

        operator fun getValue(thisRef: K, desc: KProperty<*>): V? = get(thisRef)

        operator fun setValue(thisRef: K, desc: KProperty<*>, value: V?) {
            set(thisRef, value)
        }
    }
}

open class DefaultMapping : Mapping {

    override val defaultArgumentsDispatchFunction: Mapping.Delegate<IrFunction, IrFunction> = newMapping()
    override val defaultArgumentsOriginalFunction: Mapping.Delegate<IrFunction, IrFunction> = newMapping()
    override val suspendFunctionToCoroutineConstructor: Mapping.Delegate<IrFunction, IrConstructor> = newMapping()
    override val lateInitFieldToNullableField: Mapping.Delegate<IrField, IrField> = newMapping()
    override val inlineClassMemberToStatic: Mapping.Delegate<IrFunction, IrSimpleFunction> = newMapping()
    override val capturedFields: Mapping.Delegate<IrClass, Collection<IrField>> = newMapping()
    override val capturedConstructors: Mapping.Delegate<IrConstructor, IrConstructor> = newMapping()
    override val reflectedNameAccessor: Mapping.Delegate<IrClass, IrSimpleFunction> = newMapping()

    protected open fun <K : IrDeclaration, V> newMapping() = object : Mapping.Delegate<K, V>() {
        private val map: MutableMap<K, V> = mutableMapOf()

        override operator fun get(key: K): V? {
            return map[key]
        }

        override operator fun set(key: K, value: V?) {
            if (value == null) {
                map.remove(key)
            } else {
                map[key] = value
            }
        }
    }
}

fun <V : Any> KMutableProperty0<V?>.getOrPut(fn: () -> V) = this.get() ?: fn().also {
    this.set(it)
}

fun <K : IrDeclaration, V> Mapping.Delegate<K, V>.getOrPut(key: K, fn: () -> V) = this[key] ?: fn().also {
    this[key] = it
}