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

open class Mapping {
    val defaultArgumentsDispatchFunction: DeclarationMapping<IrFunction, IrFunction> by AttributeBasedMappingDelegate()
    val defaultArgumentsOriginalFunction: MapBasedMapping<IrFunction, IrFunction> = MapBasedMapping()
    val suspendFunctionToCoroutineConstructor: DeclarationMapping<IrFunction, IrConstructor> by AttributeBasedMappingDelegate()
    val lateInitFieldToNullableField: DeclarationMapping<IrField, IrField> by AttributeBasedMappingDelegate()
    val inlineClassMemberToStatic: DeclarationMapping<IrFunction, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val capturedFields: DeclarationMapping<IrClass, Collection<IrField>> by AttributeBasedMappingDelegate()
    val capturedConstructors: MapBasedMapping<IrConstructor, IrConstructor> = MapBasedMapping()
    val reflectedNameAccessor: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val suspendFunctionsToFunctionWithContinuations: DeclarationMapping<IrSimpleFunction, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val functionWithContinuationsToSuspendFunctions: DeclarationMapping<IrSimpleFunction, IrSimpleFunction> by AttributeBasedMappingDelegate()

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

fun <V : Any> KMutableProperty0<V?>.getOrPut(fn: () -> V) = this.get() ?: fn().also {
    this.set(it)
}

fun <K : IrDeclaration, V> Mapping.DeclarationMapping<K, V>.getOrPut(key: K, fn: () -> V) = this[key] ?: fn().also {
    this[key] = it
}