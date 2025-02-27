/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import com.sun.corba.se.impl.encoding.CodeSetConversion.impl
import org.jetbrains.kotlin.generators.tree.AbstractField.ImplementationDefaultStrategy
import org.jetbrains.kotlin.generators.tree.Model
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.elementDescendantsDepthFirst
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ListField
import kotlin.collections.iterator

fun assignCoreAttributeIds(model: Model<Element>) {
    val elements = model.elements.sortedBy { it.elementDescendantsDepthFirst().count() }
    val implementations = elements.flatMap { it.implementations }
    val fieldsByName = buildList {
        implementations.forEach { impl ->
            impl.allFields.forEach {
                add(impl to it)
            }
        }
    }.groupBy { Pair(it.second.name, it.second.typeRef == StandardTypes.boolean) }

    var maxAllocatedId = -1
    for (allocFlags in arrayOf(false, true)) {
        for ((group, fieldsWithImpl) in fieldsByName) {
            if (group.second != allocFlags) continue

            var id = -1
            selectId@ while (true) {
                for ((impl, _) in fieldsWithImpl) {
                    val allocatedBitSet = impl.allocatedAttributes
                    if (id == -1 || allocatedBitSet[id]) {
                        id = if (allocFlags) allocatedBitSet.previousClearBit(if (id == -1) 63 else id - 1)
                        else allocatedBitSet.nextClearBit(id + 1)
                        continue@selectId
                    }
                }
                break@selectId
            }

            if (allocFlags) require(id in (64 - MAX_FLAG_ATTRIBUTES)..<64)
            else require(id in 0..<(64 - MAX_FLAG_ATTRIBUTES))
            maxAllocatedId = maxOf(maxAllocatedId, id)
            for ((impl, field) in fieldsWithImpl) {
                field.id = id
                val allocatedBitSet = impl.allocatedAttributes
                allocatedBitSet[id] = true
            }
        }
    }

    println("Max allocated ID: $maxAllocatedId")
}

fun calculatePreallocatedCoreAttributeStorageSize(model: Model<Element>) {
    for (element in model.elements) {
        for (implementation in element.implementations) {
            var size = implementation.fieldsInConstructor.count { it.typeRef != StandardTypes.boolean }
            val bodyAttrs = implementation.fieldsInBody.filter { it.typeRef != StandardTypes.boolean }

            for (field in bodyAttrs) {
                val defaultValue = field.implementationDefaultStrategy as? ImplementationDefaultStrategy.DefaultValue ?: continue
                if (!defaultValue.withGetter && defaultValue.defaultValue != "this" && !(field is ListField && field.mutability == ListField.Mutability.MutableList)) {
                    field.useSharedDefaultValues = true
                }
            }

            val (defaultAttrs, storedAttrs) = bodyAttrs.partition { it.useSharedDefaultValues }
            size += storedAttrs.size + (defaultAttrs.size * 0.25).toInt()

            implementation.preallocateStorageSize = size
        }
    }
}

// Sync with IrElementBase.MAX_FLAG_ATTRIBUTES
private const val MAX_FLAG_ATTRIBUTES = 10