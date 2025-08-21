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
 * Creates new [IrAttribute] which can be used to store additional data of type [T] inside of [E].
 *
 * See [IrAttribute] for details.
 *
 * @param copyByDefault Whether to copy this attribute in [IrElement.copyAttributes] by default.
 * If [false], it will only be copied when specifying `copyAttributes(other, includeAll = true)`.
 */
fun <E : IrElement, T : Any> irAttribute(copyByDefault: Boolean): IrAttribute.Delegate<E, T> =
    IrAttribute.Delegate(copyByDefault)

/**
 * Creates new [IrAttribute] which can be used to put an additional mark
 * on an element of type [E].
 *
 * This is similar to using `irAttribute<E, Boolean>()`, except:
 * - Boolean attribute has 3 states: `false`, `true` and `null`,
 * while flag has 2: set or not set.
 * - It is possible to store a flag a bit more efficiently,
 * by not allocating a slot for a value - not implemented yet.
 *
 * See [irAttribute] for details.
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
 * Example usage:
 * ```kotlin
 * var IrFunction.binaryName: String by irAttribute()
 *
 * fun computeBinaryName(function: IrFunction) {
 *     function.binaryName = function.findBinaryNameAnnotation() ?: function.name.mangle()
 * }
 * ```
 *
 * ##### Migration note:
 * There is also a [MutableMap] wrapper around [IrAttribute] to ease migration from maps in
 * the form of `MutableMap<IrElement, T>` to attributes - you can replace such maps with
 * `irAttribute().asMap()` in-place, and the usual map-based syntax will continue to work.
 * Example:
 * ```kotlin
 * val binaryNames = mutableMapOf<IrDeclaration, String>()
 * // can be replaced with:
 * val binaryNames by irAttribute<IrDeclaration, String>().asMap()
 * ```
 * `binaryNames[element]` will then behave the same way as `element.binaryNames`.
 *
 * Similarly, `MutableSet<IrElement>` may be implemented by `irFlag()`:
 * ```kotlin
 * val functionsWithSpecialBridges = hashSet<IrFunction>()
 * // can be replaced with:
 * val functionsWithSpecialBridges by irFlag<IrFunction>().asSet()
 * ```
 * This is useful for marker sets, e.g. where later you would check `someFunction in functionsWithSpecialBridges`.
 *
 * However, a proper migration to the extension property syntax is eventually expected, and this
 * functionality is to be removed.
 * Note that collection operations, like iterating over all entries of the map, are not supported
 * and will throw at runtime. If there is a need to use them, such a map should not be converted to
 * an attribute.
 *
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

            fun asSet() = PropertyDelegateProvider { thisRef: Any?, property: KProperty<*> ->
                val attribute = this@Delegate.provideDelegate(thisRef, property)
                DummyDelegate(IrAttributeMapWrapper.FlagSetWrapper(attribute))
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


        fun asMap() = PropertyDelegateProvider { thisRef: Any?, property: KProperty<*> ->
            val attribute = this@Delegate.provideDelegate(thisRef, property)
            DummyDelegate(IrAttributeMapWrapper(attribute))
        }
    }
}

/**
 * A helper for migration from `MutableMap<IrElement, T>` to `IrAttribute`.
 * See [IrAttribute]
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrAttributeMapWrapper<E : IrElement, T : Any> internal constructor(
    val attribute: IrAttribute<E, T>,
) : AbstractMutableMap<E, T>() {
    override operator fun get(element: E): T? {
        return element[attribute]
    }

    override fun containsKey(element: E): Boolean {
        return element[attribute] != null
    }

    override fun put(element: E, value: T): T? {
        return element.set(attribute, value)
    }

    override fun remove(element: E): T? {
        return element.set(attribute, null)
    }

    override fun computeIfAbsent(element: E, mappingFunction: Function<in E, out T>): T {
        element[attribute]?.let {
            return it
        }
        val newValue = mappingFunction.apply(element)
        element[attribute] = newValue
        return newValue
    }

    override val keys: MutableSet<E> = KeyCollection()

    @Deprecated(
        "Not implemented in IrAttribute, will throw at runtime." +
                "If you need this Map functionality, please use regular MutableMap.",
        level = DeprecationLevel.ERROR
    )
    override val entries: MutableSet<MutableMap.MutableEntry<E, T>> get() = unsupportedMapOperation()

    @Deprecated(
        "Not implemented in IrAttribute, will throw at runtime." +
                "If you need this Map functionality, please use regular MutableMap.",
        level = DeprecationLevel.ERROR
    )
    override val size: Int get() = unsupportedMapOperation()

    @Deprecated(
        "Not implemented in IrAttribute, will throw at runtime." +
                "If you need this Map functionality, please use regular MutableMap.",
        level = DeprecationLevel.ERROR
    )
    override fun clear() = unsupportedMapOperation()

    override fun equals(other: Any?): Boolean = other is IrAttributeMapWrapper<*, *> && attribute == other.attribute

    override fun hashCode(): Int = attribute.hashCode()

    override fun toString(): String = attribute.toString()


    private inner class KeyCollection : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return element[attribute] != null
        }

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override val size: Int
            get() = unsupportedMapOperation()

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override fun add(element: E): Boolean = unsupportedMapOperation()

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override fun iterator(): MutableIterator<E> = unsupportedMapOperation()
    }

    /**
     * A helper for migration from `MutableSet<IrElement>` to `IrAttribute`.
     * See [IrAttribute]
     */
    class FlagSetWrapper<E : IrElement> internal constructor(
        private val flag: IrAttribute.Flag<E>,
    ) : AbstractMutableSet<E>() {
        override fun contains(element: E): Boolean {
            return flag.get(element)
        }

        override fun add(element: E): Boolean {
            val wasSet = flag.get(element)
            flag.set(element, true)
            return !wasSet
        }

        override fun remove(element: E): Boolean {
            val wasSet = flag.get(element)
            flag.set(element, false)
            return wasSet
        }

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override val size: Int get() = unsupportedMapOperation()

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override fun iterator(): MutableIterator<E> = unsupportedMapOperation()

        @Deprecated(
            "Not implemented in IrAttribute, will throw at runtime." +
                    "If you need this Map functionality, please use regular MutableMap.",
            level = DeprecationLevel.ERROR
        )
        override fun clear() = unsupportedMapOperation()
    }

    companion object {
        private fun unsupportedMapOperation(): Nothing =
            throw UnsupportedOperationException("This map-based operation is unsupported by IR attribute")
    }
}

