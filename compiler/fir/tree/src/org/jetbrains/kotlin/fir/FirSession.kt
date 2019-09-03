/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FirSession(val sessionProvider: FirSessionProvider?) {
    open val moduleInfo: ModuleInfo? get() = null

    val jsr305State: Jsr305State? get() = null


    val components: MutableMap<KClass<*>, Any> = mutableMapOf()

    internal val componentArray = ComponentArray()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(kclass: KClass<T>): T =
        components[kclass] as T

    protected fun <T : Any /* TODO: FirSessionComponent */> registerComponent(tClass: KClass<T>, t: T) {
        assert(tClass !in components) { "Already registered component" }
        components[tClass] = t

        // TODO: Make t of FirSessionComponent
        if (t is FirSessionComponent) {
            @Suppress("UNCHECKED_CAST")
            componentArray[(tClass as KClass<FirSessionComponent>).componentId()] = t
        }
    }
}

interface FirSessionProvider {
    fun getSession(moduleInfo: ModuleInfo): FirSession?
}

inline fun <reified T : Any> FirSession.service(): T =
    getService(T::class)

internal object ComponentTypeRegistry {
    private val idPerType = mutableMapOf<KClass<out FirSessionComponent>, Int>()

    fun <T : FirSessionComponent> id(kClass: KClass<T>): Int {
        return idPerType.getOrPut(kClass) { idPerType.size }
    }
}


private fun <T : FirSessionComponent> KClass<T>.componentId(): Int {
    return ComponentTypeRegistry.id(this)
}


class ComponentArrayAccessor<T : FirSessionComponent>(val type: KClass<T>) : ReadOnlyProperty<FirSession, T> {
    val id: Int = type.componentId()
    override fun getValue(thisRef: FirSession, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return thisRef.componentArray.getOrNull(id) as? T ?: error("No '$type'($id) component in session: $thisRef")
    }
}

inline fun <reified T : FirSessionComponent> componentArrayAccessor(): ComponentArrayAccessor<T> {
    return ComponentArrayAccessor(T::class)
}

interface FirSessionComponent

internal class ComponentArray : AbstractList<FirSessionComponent?>() {
    override val size: Int
        get() = data.size
    private var data = arrayOfNulls<FirSessionComponent>(20)
    private fun ensureCapacity(index: Int) {
        if (data.size < index) {
            data = data.copyOf(data.size * 2)
        }
    }

    operator fun set(index: Int, value: FirSessionComponent) {
        ensureCapacity(index)
        data[index] = value
    }

    override operator fun get(index: Int): FirSessionComponent? {
        return data[index]
    }
}