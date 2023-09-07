/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.FieldContainer
import org.jetbrains.kotlin.generators.tree.Importable
import org.jetbrains.kotlin.generators.tree.generics

private const val DEFAULT_BUILDER_PACKAGE = "org.jetbrains.kotlin.fir.tree.builder"

sealed class Builder : FieldContainer, Importable {
    val parents: MutableList<IntermediateBuilder> = mutableListOf()
    val usedTypes: MutableList<Importable> = mutableListOf()
    abstract override val allFields: List<FieldWithDefault>
    abstract val uselessFields: List<FieldWithDefault>

    abstract override val packageName: String

    override fun get(fieldName: String): FieldWithDefault {
        return allFields.firstOrNull { it.name == fieldName }
            ?: throw IllegalArgumentException("Builder $type doesn't contains field $fieldName")
    }

    private val fieldsFromParentIndex: Map<String, Boolean> by lazy {
        mutableMapOf<String, Boolean>().apply {
            for (field in allFields + uselessFields) {
                this[field.name] = parents.any { field.name in it.allFields.map { it.name } }
            }
        }
    }

    fun isFromParent(field: Field): Boolean = fieldsFromParentIndex.getValue(field.name)
}

class LeafBuilder(val implementation: Implementation) : Builder() {
    override val type: String
        get() = if (implementation.name != null) {
            "${implementation.name}Builder"
        } else {
            "${implementation.element.type}Builder"
        }

    override fun getTypeWithArguments(notNull: Boolean): String = type + implementation.element.generics

    override val allFields: List<FieldWithDefault> by lazy { implementation.fieldsWithoutDefault }

    override val uselessFields: List<FieldWithDefault> by lazy {
        val fieldsFromParents = parents.flatMap { it.allFields }.distinct()
        val fieldsFromImplementation = implementation.allFields
        (fieldsFromImplementation - allFields).filter { it in fieldsFromParents }
    }

    override val packageName: String = implementation.packageName.replace(".impl", ".builder")
    var isOpen: Boolean = false
    var wantsCopy: Boolean = false
}

class IntermediateBuilder(override val type: String) : Builder() {
    val fields: MutableList<FieldWithDefault> = mutableListOf()
    var materializedElement: Element? = null

    override val allFields: List<FieldWithDefault> by lazy {
        mutableSetOf<FieldWithDefault>().apply {
            parents.forEach { this += it.allFields }
            this += fields
        }.toList()
    }

    override val uselessFields: List<FieldWithDefault> = emptyList()
    override var packageName: String = DEFAULT_BUILDER_PACKAGE

    override fun getTypeWithArguments(notNull: Boolean): String = type
}
