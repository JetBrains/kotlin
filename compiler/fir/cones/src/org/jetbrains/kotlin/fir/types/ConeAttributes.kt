/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.utils.AttributeArrayOwner
import org.jetbrains.kotlin.fir.utils.Protected
import org.jetbrains.kotlin.fir.utils.TypeRegistry
import org.jetbrains.kotlin.fir.utils.isEmpty
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

abstract class ConeAttribute<T : ConeAttribute<T>> {
    abstract fun union(other: @UnsafeVariance T?): T?
    abstract fun intersect(other: @UnsafeVariance T?): T?
    abstract fun isSubtypeOf(other: @UnsafeVariance T?): Boolean

    abstract override fun toString(): String

    abstract val key: KClass<out T>
}

@OptIn(Protected::class)
class ConeAttributes private constructor(attributes: List<ConeAttribute<*>>) : AttributeArrayOwner<ConeAttribute<*>, ConeAttribute<*>>(),
    Iterable<ConeAttribute<*>> {

    companion object : TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>() {
        inline fun <reified T : ConeAttribute<T>> attributeAccessor(): ReadOnlyProperty<ConeAttributes, T?> {
            @Suppress("UNCHECKED_CAST")
            return generateNullableAccessor<ConeAttribute<*>, T>(T::class) as ReadOnlyProperty<ConeAttributes, T?>
        }

        val Empty: ConeAttributes = ConeAttributes(emptyList())

        fun create(attributes: List<ConeAttribute<*>>): ConeAttributes {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                ConeAttributes(attributes)
            }
        }
    }

    init {
        for (attribute in attributes) {
            registerComponent(attribute.key, attribute)
        }
    }

    fun union(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.union(it) }
    }

    fun intersect(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.intersect(it) }
    }

    override fun iterator(): Iterator<ConeAttribute<*>> {
        return arrayMap.iterator()
    }

    private inline fun perform(other: ConeAttributes, op: ConeAttribute<*>.(ConeAttribute<*>?) -> ConeAttribute<*>?): ConeAttributes {
        if (this.isEmpty() && other.isEmpty()) return this
        val attributes = mutableListOf<ConeAttribute<*>>()
        for (index in indices) {
            val a = arrayMap[index]
            val b = other.arrayMap[index]
            val res = when {
                a == null -> b?.op(a)
                else -> a.op(b)
            }
            attributes.addIfNotNull(res)
        }
        return create(attributes)
    }

    override val typeRegistry: TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>
        get() = Companion

    private fun isEmpty(): Boolean {
        return arrayMap.isEmpty()
    }
}
