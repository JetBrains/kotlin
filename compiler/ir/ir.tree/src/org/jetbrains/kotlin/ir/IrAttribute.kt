/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.utils.DummyDelegate
import java.lang.ref.WeakReference
import java.util.function.Function
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty

/**
 * Creates new [IrAttribute] which can be used to store additional data of type [T] inside of [E]. Designed to use as delegate, e.g.:
 * ```
 * var IrFunction.binaryName: String? by irAttribute()
 * ```
 *
 * ## When to use
 *
 * Here's a general guideline on when to choose which mechanism of associating data with [IrElement]:
 *
 * - [irAttribute]
 * 1. When the data is only used in one of the Kotlin backends.
 *    In that case, define it in either one of the dedicated files (`JvmIrAttributes.kt`, `JsIrAttributes.kt` etc.)
 *    or close to the primary usage.
 * 2. For "auxiliary" data / caches.
 * 3. When the data is expected to be `null` most of the time. `null` values are optimized away by [irAttribute].
 * - Regular properties (defined in [org.jetbrains.kotlin.ir.generator.IrTree])
 * 1. For the "primary" kind of data, which constitutes the given [IrElement]. E.g.: `IrFunction.name`.
 * 2. The above can generally be restated as "the data that should be serialized into Klib".
 * - `MutableMap<IrElement, T>`
 * 1. When the data is used in a single scope, e.g. inside a single class, function.
 * 2. When the data is used in a single lowering phase.
 * 3. When you have to enumerate over all elemenets with this data.
 *
 * @param copyByDefault Whether to copy this attribute in [IrElement.copyAttributes] by default.
 * If `false`, it will only be copied when specifying `copyAttributes(other, includeAll = true)`.
 */
fun <E : IrElement, T : Any> irAttribute(copyByDefault: Boolean): IrAttribute.Delegate<E, T> =
    IrAttribute.Delegate(copyByDefault)

/**
 * Creates new [IrAttribute] which can be used to put an additional mark on an [IrElement] of type [E]. Designed to use as delegate, e.g.:
 * ```
 * var IrFunction.isPublicAbi: Boolean by irFlag()
 * ```
 * ## When to use
 * See [irAttribute].
 *
 * ## [irFlag] vs [irAttribute]
 * [irFlag] is similar to `irAttribute<E, Boolean>()`, except:
 * - Boolean attribute has 3 states: `false`, `true` and `null`,
 * while a flag has 2: set or not set.
 * - It is possible to store a flag in memory a bit more efficiently.
 *
 */
fun <E : IrElement> irFlag(copyByDefault: Boolean): IrAttribute.Flag.Delegate<E> =
    IrAttribute.Flag.Delegate<E>(IrAttribute.Delegate<E, Boolean>(copyByDefault))


/**
 * Returns a value of [attribute], or null if the value is missing.
 */
operator fun <E : IrElement, T : Any> E.get(attribute: IrAttribute<E, T>): T? {
    return (this as IrElementBase).getAttributeInternal(attribute)
}

/**
 * Stores a [value] associated with [attribute] in this IrElement, or removes an association if [value] is null.
 *
 * @return The previous value associated with the attribute, or null if the attribute was not present.
 */
operator fun <E : IrElement, T : Any> E.set(attribute: IrAttribute<E, T>, value: T?): T? {
    return (this as IrElementBase).setAttributeInternal(attribute, value)
}

/**
 * A key for storing additional data inside [IrElement].
 *
 * @see [irAttribute]
 * @param E restricts the type of [IrElement] on which this attribute can be stored.
 * @param T the type of the data stored in the attribute.
 */
class IrAttribute<E : IrElement, T : Any> internal constructor(
    val name: String?,
    owner: Any?,
    val copyByDefault: Boolean,
) {
    /**
     * Used solely for debug, to help distinguish between multiple instances of attribute keys.
     * This may happen if the key is defined inside some class, instead of on top level.
     */
    val ownerForDebug = owner?.let(::WeakReference)

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: E, property: KProperty<*>): T? {
        return thisRef[this]
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun setValue(thisRef: E, property: KProperty<*>, value: T?) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return when {
            name != null && ownerForDebug?.get() != null -> "$name (inside of ${ownerForDebug.get()})"
            name != null -> name
            else -> super.toString()
        }
    }

    /**
     * See [irFlag]
     */
    class Flag<E : IrElement> internal constructor(
        private val attribute: IrAttribute<E, Boolean>,
    ) {
        @Suppress("NOTHING_TO_INLINE")
        inline operator fun getValue(thisRef: E, property: KProperty<*>): Boolean = get(thisRef)

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun setValue(thisRef: E, property: KProperty<*>, value: Boolean) = set(thisRef, value)

        fun get(element: E): Boolean {
            return element[attribute] == true
        }

        fun set(element: E, value: Boolean) {
            element[attribute] = if (value) true else null
        }

        class Delegate<E : IrElement> internal constructor(
            private val attributeDelegate: IrAttribute.Delegate<E, Boolean>,
        ) {
            operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Flag<E> {
                val attribute = attributeDelegate.provideDelegate(thisRef, property)
                return Flag(attribute)
            }
        }
    }

    class Delegate<E : IrElement, T : Any> internal constructor(
        private val copyByDefault: Boolean,
    ) {
        fun create(owner: Any?, name: String?): IrAttribute<E, T> {
            return IrAttribute(
                name = name,
                owner = owner,
                copyByDefault = copyByDefault,
            )
        }

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): IrAttribute<E, T> =
            create(thisRef, property.name)
    }
}