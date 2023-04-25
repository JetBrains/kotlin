/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.types.model.AnnotationMarker
import org.jetbrains.kotlin.util.AttributeArrayOwner
import org.jetbrains.kotlin.util.TypeRegistry
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

abstract class ConeAttribute<out T : ConeAttribute<T>> : AnnotationMarker {
    abstract fun union(other: @UnsafeVariance T?): T?
    abstract fun intersect(other: @UnsafeVariance T?): T?

    /*
     * This function is used to decide how multiple attributes should be united in presence of typealiases:
     * typealias B = @SomeAttribute(1) A
     * typealias C = @SomeAttribute(2) B
     *
     * For determining attribute value of expanded type of C we should add @SomeAttribute(2) to @SomeAttribute(1)
     *
     * This function must be symmetrical: a.add(b) == b.add(a)
     */
    abstract fun add(other: @UnsafeVariance T?): T?
    abstract fun isSubtypeOf(other: @UnsafeVariance T?): Boolean

    abstract override fun toString(): String

    abstract val key: KClass<out T>
}

typealias ConeAttributeKey = KClass<out ConeAttribute<*>>

class ConeAttributes private constructor(attributes: List<ConeAttribute<*>>) : AttributeArrayOwner<ConeAttribute<*>, ConeAttribute<*>>(),
    Iterable<ConeAttribute<*>> {

    companion object : ConeTypeRegistry<ConeAttribute<*>, ConeAttribute<*>>() {
        inline fun <reified T : ConeAttribute<T>> attributeAccessor(): ReadOnlyProperty<ConeAttributes, T?> {
            @Suppress("UNCHECKED_CAST")
            return generateNullableAccessor<ConeAttribute<*>, T>(T::class) as ReadOnlyProperty<ConeAttributes, T?>
        }

        val Empty: ConeAttributes = ConeAttributes(emptyList())
        val WithExtensionFunctionType: ConeAttributes = ConeAttributes(listOf(CompilerConeAttributes.ExtensionFunctionType))

        private val predefinedAttributes: Map<ConeAttribute<*>, ConeAttributes> = mapOf(
            CompilerConeAttributes.EnhancedNullability.predefined()
        )

        private fun ConeAttribute<*>.predefined(): Pair<ConeAttribute<*>, ConeAttributes> = this to ConeAttributes(this)

        fun create(attributes: List<ConeAttribute<*>>): ConeAttributes {
            return if (attributes.isEmpty()) {
                Empty
            } else {
                ConeAttributes(attributes)
            }
        }
    }

    private constructor(attribute: ConeAttribute<*>) : this(listOf(attribute))

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

    fun add(other: ConeAttributes): ConeAttributes {
        return perform(other) { this.add(it) }
    }

    operator fun contains(attribute: ConeAttribute<*>): Boolean {
        return contains(attribute.key)
    }

    operator fun contains(attributeKey: KClass<out ConeAttribute<*>>): Boolean {
        val index = getId(attributeKey)
        return arrayMap[index] != null
    }

    operator fun plus(attribute: ConeAttribute<*>): ConeAttributes {
        if (attribute in this) return this
        if (isEmpty()) return predefinedAttributes[attribute] ?: ConeAttributes(attribute)
        val newAttributes = buildList {
            addAll(this)
            add(attribute)
        }
        return ConeAttributes(newAttributes)
    }

    fun remove(attribute: ConeAttribute<*>): ConeAttributes {
        if (isEmpty()) return this
        val attributes = arrayMap.filter { it != attribute }
        if (attributes.size == arrayMap.size) return this
        return create(attributes)
    }

    private inline fun perform(other: ConeAttributes, op: ConeAttribute<*>.(ConeAttribute<*>?) -> ConeAttribute<*>?): ConeAttributes {
        if (this.isEmpty() && other.isEmpty()) return this
        val attributes = mutableListOf<ConeAttribute<*>>()
        for (index in indices) {
            val a = arrayMap[index]
            val b = other.arrayMap[index]
            val res = if (a == null) b?.op(a) else a.op(b)
            attributes.addIfNotNull(res)
        }
        return create(attributes)
    }

    override val typeRegistry: TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>
        get() = Companion
}
