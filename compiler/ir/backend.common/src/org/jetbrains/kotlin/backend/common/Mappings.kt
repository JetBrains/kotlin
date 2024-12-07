/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.get
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.set
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

var IrFunction.defaultArgumentsDispatchFunction: IrFunction? by irAttribute(followAttributeOwner = false)

var IrClass.capturedFields: Collection<IrField>? by irAttribute(followAttributeOwner = false)

var IrClass.reflectedNameAccessor: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * If this is a `suspend` function, returns its corresponding function with continuations.
 */
var IrSimpleFunction.functionWithContinuations: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * If this is a function with continuation, returns its corresponding `suspend` function.
 */
var IrSimpleFunction.suspendFunction: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

var IrFunction.defaultArgumentsOriginalFunction: IrFunction? by irAttribute(followAttributeOwner = false)

open class Mapping {
    val capturedConstructors: MapBasedMapping<IrConstructor, IrConstructor> = MapBasedMapping()

    abstract class DeclarationMapping<K : IrDeclaration, V> {
        abstract operator fun get(declaration: K): V?
        abstract operator fun set(declaration: K, value: V?)

        operator fun getValue(thisRef: K, desc: KProperty<*>): V? = get(thisRef)

        operator fun setValue(thisRef: K, desc: KProperty<*>, value: V?) {
            set(thisRef, value)
        }
    }

    /**
     * Mapping from K to V backed by a regular MutableMap.
     * Its only use is when the access to [keys] is necessary,
     * otherwise it should be avoided.
     */
    class MapBasedMapping<K : IrDeclaration, V> : DeclarationMapping<K, V>() {
        private val map: MutableMap<K, V> = ConcurrentHashMap()

        override operator fun get(declaration: K): V? {
            return map[declaration]
        }

        override operator fun set(declaration: K, value: V?) {
            if (value == null) {
                map.remove(declaration)
            } else {
                map[declaration] = value
            }
        }

        val keys: Set<K>
            get() = map.keys
    }

    /**
     * Mapping from K to V backed by [IrAttribute].
     * Usages are to be refactored to use [IrAttribute]s directly - KT-69082.
     */
    protected class AttributeBasedMapping<K : IrDeclaration, V : Any>(
        private val attribute: IrAttribute<K, V>
    ) : DeclarationMapping<K, V>() {
        override fun get(declaration: K): V? {
            return declaration[attribute]
        }

        override fun set(declaration: K, value: V?) {
            declaration[attribute] = value
        }
    }

    protected class AttributeBasedMappingDelegate<K : IrDeclaration, V : Any> () {
        private lateinit var mapping: AttributeBasedMapping<K, V>

        operator fun provideDelegate(thisRef: Any?, desc: KProperty<*>): AttributeBasedMappingDelegate<K, V> {
            val attribute = irAttribute<K, V>(followAttributeOwner = false).provideDelegate(thisRef, desc)
            this.mapping = AttributeBasedMapping(attribute)
            return this
        }

        operator fun getValue(thisRef: Any?, desc: KProperty<*>): AttributeBasedMapping<K, V> {
            return mapping
        }
    }
}

fun <K : IrDeclaration, V> Mapping.DeclarationMapping<K, V>.getOrPut(key: K, fn: () -> V) = this[key] ?: fn().also {
    this[key] = it
}