/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import java.util.function.Function

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDynamicPropertyKey<E : IrElement, T> internal constructor() : AbstractMutableMap<E, T>() {
    //@Deprecated("Old-school syntax", ReplaceWith("element[this]"))
    override operator fun get(element: E): T? {
        return element[this]
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element[this] != null"))
    override fun containsKey(element: E): Boolean {
        return element[this] != null
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.set(this, value)"))
    operator fun set(element: E, value: T?) {
        element[this] = value
    }

    override fun put(element: E, value: T): T? {
        return element.set(this, value)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.set(this, null)"))
    override fun remove(element: E): T? {
        return element.set(this, null)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.getOrPutDynamicProperty(this, compute)"))
    fun getOrPut(element: E, compute: () -> T): T {
        return element.getOrPutDynamicProperty(this, compute)
    }

    //@Deprecated("Old-school syntax", ReplaceWith("element.getOrPutDynamicProperty(this, compute)"))
    fun computeIfAbsent(element: E, compute: (E) -> T): T {
        return element.getOrPutDynamicProperty(this) { compute(element) }
    }

    override fun computeIfAbsent(element: E, mappingFunction: Function<in E, out T>): T {
        return element.getOrPutDynamicProperty(this) { mappingFunction.apply(element) }
    }


    override val entries: MutableSet<MutableMap.MutableEntry<E, T>> get() = unsupportedMapOperation()

    override val size: Int get() = unsupportedMapOperation()

    override fun clear() = unsupportedMapOperation()


    override val keys: MutableSet<E> = KeyCollection()

    private inner class KeyCollection : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return element[this@IrDynamicPropertyKey] != null
        }

        override val size: Int
            get() = throw UnsupportedOperationException()

        override fun add(element: E): Boolean {
            throw UnsupportedOperationException()
        }

        override fun iterator(): MutableIterator<E> {
            throw UnsupportedOperationException()
        }
    }


    class SetWrapper<E : IrElement>(
        private val key: IrDynamicPropertyKey<E, Boolean>,
    ) : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return element[key] == true
        }

        override fun add(element: E): Boolean {
            return element.set(key, true) != true
        }

        override fun remove(element: E): Boolean {
            return element.set(key, null) == true
        }


        override val size: Int get() = unsupportedMapOperation()

        override fun iterator(): MutableIterator<E> = unsupportedMapOperation()

        override fun clear() = unsupportedMapOperation()
    }

    companion object {
        private fun unsupportedMapOperation(): Nothing =
            throw UnsupportedOperationException("This map-based operation is unsupported by IR dynamic property")
    }
}

fun <E : IrElement, T> createIrDynamicProperty(): IrDynamicPropertyKey<E, T> = IrDynamicPropertyKey()

fun <E : IrElement> createIrDynamicFlag(): IrDynamicPropertyKey.SetWrapper<E> =
    IrDynamicPropertyKey.SetWrapper<E>(IrDynamicPropertyKey<E, Boolean>())


operator fun <E : IrElement, T> E.get(key: IrDynamicPropertyKey<E, T>): T? {
    return (this as IrElementBase).getDynamicProperty(key)
}

operator fun <E : IrElement, T> E.set(key: IrDynamicPropertyKey<E, T>, value: T?): T? {
    return (this as IrElementBase).setDynamicProperty(key, value)
}

fun <E : IrElement, T> E.getOrPutDynamicProperty(key: IrDynamicPropertyKey<E, T>, compute: () -> T): T {
    return (this as IrElementBase).getOrPutDynamicProperty(key, compute)
}

/*
fun <E : IrElement, T> E.getOrPutDynamicProperty(token: IrDynamicPropertyKey<E, T>, compute: (E) -> T): T {
    return (this as IrElementBase).getOrPutDynamicProperty(token) { compute(this) }
}*/
