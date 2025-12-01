/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import kotlin.reflect.KProperty

/**
 * Creates a new [KlibAttribute] which can be used to store additional data of type [T] inside of [Klib].
 * Designed to be used as a delegate, e.g.:
 * ```
 * var Klib.fingerprintHash: FingerprintHash? by klibAttribute()
 * ```
 */
fun <T : Any> klibAttribute(): KlibAttribute.Delegate<T> = KlibAttribute.Delegate()

/**
 * Creates a new [KlibAttribute] which can be used to store a boolean flag in an [Klib].
 * Designed to be used as a delegate, e.g.:
 * ```
 * var Klib.isDefault: Boolean by klibFlag()
 * ```
 */
fun klibFlag(): KlibAttribute.Flag.Delegate = KlibAttribute.Flag.Delegate()

/**
 * A container to store additional data for [Klib]s that can be arbitrarily read and written.
 *
 * The stored data is not serialized into the library artifact. It is associated with the current
 * [Klib] instance and exists only during the lifetime of this [Klib] instance.
 *
 * See [Klib.attributes].
 */
class KlibAttributes internal constructor() {

    private val attributes: MutableMap<KlibAttribute<*>, Any> = hashMapOf()

    operator fun <T : Any> get(attribute: KlibAttribute<T>): T? {
        val value: Any? = attributes[attribute]

        @Suppress("UNCHECKED_CAST")
        return value as T?
    }

    operator fun <T : Any> set(attribute: KlibAttribute<T>, value: T?): T? {
        val oldValue: Any? = if (value != null)
            attributes.put(attribute, value)
        else
            attributes.remove(attribute)

        @Suppress("UNCHECKED_CAST")
        return oldValue as T?
    }
}

/**
 * A key for storing additional data inside [KlibAttributes].
 * See [klibAttribute].
 */
class KlibAttribute<T : Any> private constructor(val name: String) {
    override fun toString(): String = name

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Klib, property: KProperty<*>): T? {
        return thisRef.attributes[this]
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: Klib, property: KProperty<*>, value: T?) {
        thisRef.attributes[this] = value
    }

    class Delegate<T : Any> internal constructor() {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): KlibAttribute<T> = KlibAttribute(property.name)
    }

    /**
     * A key for storing additional boolean data inside [KlibAttributes].
     * See [klibFlag].
     */
    class Flag private constructor(val wrappedAttribute: KlibAttribute<Boolean>) {
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun getValue(thisRef: Klib, property: KProperty<*>): Boolean {
            return thisRef.attributes[wrappedAttribute] == true
        }

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun setValue(thisRef: Klib, property: KProperty<*>, value: Boolean) {
            thisRef.attributes[wrappedAttribute] = if (value) true else null
        }

        class Delegate internal constructor() {
            operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Flag = Flag(KlibAttribute(property.name))
        }
    }
}
