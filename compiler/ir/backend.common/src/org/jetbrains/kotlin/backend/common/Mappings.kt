/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.*
import java.util.concurrent.ConcurrentHashMap
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
    val suspendFunctionsToFunctionWithContinuations: Delegate<IrSimpleFunction, IrSimpleFunction>
    val functionWithContinuationsToSuspendFunctions: Delegate<IrSimpleFunction, IrSimpleFunction>

    abstract class Delegate<K : IrDeclaration, V> {
        abstract operator fun get(key: K): V?

        abstract operator fun set(key: K, value: V?)

        operator fun getValue(thisRef: K, desc: KProperty<*>): V? = get(thisRef)

        operator fun setValue(thisRef: K, desc: KProperty<*>, value: V?) {
            set(thisRef, value)
        }

        abstract val keys: Set<K>
    }
}

interface DelegateFactory {
    fun <K : IrDeclaration, V : IrDeclaration> newDeclarationToDeclarationMapping(): Mapping.Delegate<K, V>

    fun <K : IrDeclaration, V : Collection<IrDeclaration>> newDeclarationToDeclarationCollectionMapping(): Mapping.Delegate<K, V>
}

object DefaultDelegateFactory : DelegateFactory {
    fun <K : IrDeclaration, V> newDeclarationToValueMapping(): Mapping.Delegate<K, V> = newMappingImpl()

    override fun <K : IrDeclaration, V : IrDeclaration> newDeclarationToDeclarationMapping(): Mapping.Delegate<K, V> = newMappingImpl()

    override fun <K : IrDeclaration, V : Collection<IrDeclaration>> newDeclarationToDeclarationCollectionMapping(): Mapping.Delegate<K, V> = newMappingImpl()

    private fun <K : IrDeclaration, V> newMappingImpl() = object : Mapping.Delegate<K, V>() {
        private val map: MutableMap<K, V> = ConcurrentHashMap()

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

        override val keys: Set<K>
            get() = map.keys
    }
}

open class DefaultMapping(delegateFactory: DelegateFactory = DefaultDelegateFactory) : Mapping {
    override val defaultArgumentsDispatchFunction: Mapping.Delegate<IrFunction, IrFunction> = delegateFactory.newDeclarationToDeclarationMapping()
    override val defaultArgumentsOriginalFunction: Mapping.Delegate<IrFunction, IrFunction> = delegateFactory.newDeclarationToDeclarationMapping()
    override val suspendFunctionToCoroutineConstructor: Mapping.Delegate<IrFunction, IrConstructor> = delegateFactory.newDeclarationToDeclarationMapping()
    override val lateInitFieldToNullableField: Mapping.Delegate<IrField, IrField> = delegateFactory.newDeclarationToDeclarationMapping()
    override val inlineClassMemberToStatic: Mapping.Delegate<IrFunction, IrSimpleFunction> = delegateFactory.newDeclarationToDeclarationMapping()
    override val capturedFields: Mapping.Delegate<IrClass, Collection<IrField>> = delegateFactory.newDeclarationToDeclarationCollectionMapping()
    override val capturedConstructors: Mapping.Delegate<IrConstructor, IrConstructor> = delegateFactory.newDeclarationToDeclarationMapping()
    override val reflectedNameAccessor: Mapping.Delegate<IrClass, IrSimpleFunction> = delegateFactory.newDeclarationToDeclarationMapping()
    override val suspendFunctionsToFunctionWithContinuations: Mapping.Delegate<IrSimpleFunction, IrSimpleFunction> = delegateFactory.newDeclarationToDeclarationMapping()
    override val functionWithContinuationsToSuspendFunctions: Mapping.Delegate<IrSimpleFunction, IrSimpleFunction> = delegateFactory.newDeclarationToDeclarationMapping()
}

fun <V : Any> KMutableProperty0<V?>.getOrPut(fn: () -> V) = this.get() ?: fn().also {
    this.set(it)
}

fun <K : IrDeclaration, V> Mapping.Delegate<K, V>.getOrPut(key: K, fn: () -> V) = this[key] ?: fn().also {
    this[key] = it
}