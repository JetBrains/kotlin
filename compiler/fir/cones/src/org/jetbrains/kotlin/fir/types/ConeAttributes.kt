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

    /**
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
    open fun renderForReadability(): String = toString()

    abstract val key: KClass<out T>

    /**
     * This property indicates whether this attribute should be kept when inferring declaration return type.
     */
    abstract val keepInInferredDeclarationType: Boolean
}

/**
 * An attribute that contains a [ConeKotlinType].
 * It is assumed that the [coneType] in this attribute is somehow related to the type it is attached to.
 * Therefore, when the type is transformed (e.g., substitution, making not-null), the same transformation is applied to the attribute.
 */
abstract class ConeAttributeWithConeType<out T : ConeAttributeWithConeType<T>> : ConeAttribute<T>() {
    abstract val coneType: ConeKotlinType

    abstract fun copyWith(newType: ConeKotlinType): T
}

inline fun <T : ConeAttributeWithConeType<T>> ConeAttributeWithConeType<T>.transformOrNull(
    transform: (ConeKotlinType) -> ConeKotlinType?,
): ConeAttributeWithConeType<T>? {
    val transformedType = transform(coneType) ?: return null
    if (transformedType == coneType) return this
    return copyWith(transformedType)
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
            addAll(arrayMap)
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

    fun filterNecessaryToKeep(): ConeAttributes {
        return if (all { it.keepInInferredDeclarationType }) this
        else create(filter { it.keepInInferredDeclarationType })
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

    /**
     * Applies the [transform] to all attributes that are subtypes of [ConeAttributeWithConeType] and returns a [ConeAttributes]
     * with the results of transforms that were not-`null` or `null` if no attributes were transformed.
     */
    inline fun transformTypesWith(transform: (ConeKotlinType) -> ConeKotlinType?): ConeAttributes? {
        if (isEmpty()) return null

        // List will be allocated on demand
        var newList: MutableList<ConeAttribute<*>>? = null
        var hasDifference = false

        for ((i, attr) in this.withIndex()) {
            if (attr !is ConeAttributeWithConeType) continue
            val substitutedAttribute = attr.transformOrNull(transform) ?: continue
            if (newList == null) {
                newList = this.toMutableList()
            }
            newList[i] = substitutedAttribute
            hasDifference = hasDifference || substitutedAttribute != attr
        }

        if (newList != null && !hasDifference) {
            return this
        }

        return newList?.let(Companion::create)
    }

    override val typeRegistry: TypeRegistry<ConeAttribute<*>, ConeAttribute<*>>
        get() = Companion
}
