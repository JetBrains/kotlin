/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import kotlin.reflect.KProperty

sealed class IrAttribute<T>(val name: String) {
    override fun toString(): String = name
}

sealed class IrIndexBasedAttributeBase<T>(
    val registry: IrIndexBasedAttributeRegistry,
    val id: Int,
    name: String,
) : IrAttribute<T>(name) {
    internal val bitMask = 1L shl id

    override fun toString(): String = "$name ($id)"
}

@Suppress("NOTHING_TO_INLINE")
class IrIndexBasedAttribute<T>(
    registry: IrIndexBasedAttributeRegistry,
    id: Int,
    name: String,
    val defaultValue: T?,
) : IrIndexBasedAttributeBase<T>(registry, id, name) {
    internal val prefixMask = if (id == 0) 0L else -1L ushr (64 - id)

    inline operator fun getValue(thisRef: IrElementBase, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return thisRef.getAttributeInternal(this) ?: defaultValue as T
    }

    inline operator fun setValue(thisRef: IrElementBase, property: KProperty<*>, value: T) {
        thisRef.setAttributeInternal(this, if (value === defaultValue) null else value)
    }
}

@Suppress("NOTHING_TO_INLINE")
class IrIndexBasedFlag(
    registry: IrIndexBasedAttributeRegistry,
    id: Int,
    name: String,
) : IrIndexBasedAttributeBase<Boolean>(registry, id, name) {
    inline operator fun getValue(thisRef: IrElementBase, property: KProperty<*>): Boolean {
        return thisRef.getFlagInternal(this)
    }

    inline operator fun setValue(thisRef: IrElementBase, property: KProperty<*>, value: Boolean) {
        thisRef.setFlagInternal(this, value)
    }
}

object IrIndexBasedAttributeRegistry {
    private val attributesPerClass = AttributesPerClass()

    private fun registerAttribute(element: Class<*>, attribute: IrIndexBasedAttributeBase<*>) {
        require(attribute.id < 64) { "Too many index-based attributes" }
        val attributePerIndex = attributesPerClass.get(element)
        require(attributePerIndex.getOrNull(attribute.id) == null) { "Attribute with id=${attribute.id} is already registered" }

        repeat(attribute.id - attributePerIndex.size + 1) {
            attributePerIndex.add(null)
        }
        attributePerIndex[attribute.id] = attribute
    }

    fun <T> createAttr(element: Class<*>, id: Int, name: String, defaultValue: T?): IrIndexBasedAttribute<T> {
        val attribute = IrIndexBasedAttribute<T>(this, id, name, defaultValue)
        registerAttribute(element, attribute)
        return attribute
    }

    fun createFlag(element: Class<*>, id: Int, name: String): IrIndexBasedFlag {
        val attribute = IrIndexBasedFlag(this, id, name)
        registerAttribute(element, attribute)
        return attribute
    }

    fun getById(element: Class<*>, id: Int): IrIndexBasedAttributeBase<*> = attributesPerClass.get(element)[id]!!

    private class AttributesPerClass : ClassValue<ArrayList<IrIndexBasedAttributeBase<*>?>>() {
        override fun computeValue(type: Class<*>): ArrayList<IrIndexBasedAttributeBase<*>?> = ArrayList(16)
    }
}
