/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

sealed class Builder<BuilderField, Element> : FieldContainer<BuilderField>, TypeRefWithNullability, Importable
        where BuilderField : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {

    val parents: MutableList<IntermediateBuilder<BuilderField, Element>> = mutableListOf()

    val usedTypes: MutableList<Importable> = mutableListOf()

    abstract val uselessFields: List<BuilderField>

    override fun get(fieldName: String): BuilderField {
        return allFields.firstOrNull { it.name == fieldName }
            ?: throw IllegalArgumentException("Builder $typeName doesn't contains field $fieldName")
    }

    private val fieldsFromParentIndex: Map<String, Boolean> by lazy {
        mutableMapOf<String, Boolean>().apply {
            for (field in allFields + uselessFields) {
                this[field.name] = parents.any { field.name in it.allFields.map { it.name } }
            }
        }
    }

    fun isFromParent(field: AbstractField<*>): Boolean = fieldsFromParentIndex.getValue(field.name)

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        addImport(this)
        appendable.append(typeName)
    }

    override val nullable: Boolean
        get() = false

    override fun copy(nullable: Boolean) = this
}

class LeafBuilder<BuilderField, Element, Implementation>(
    val implementation: Implementation,
) : Builder<BuilderField, Element>()
        where BuilderField : AbstractField<*>,
              Element : AbstractElement<Element, *, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, BuilderField> {
    override val typeName: String
        get() = (implementation.name ?: implementation.element.typeName) + "Builder"

    override val allFields: List<BuilderField> by lazy { implementation.fieldsWithoutDefault }

    override val uselessFields: List<BuilderField> by lazy {
        val fieldsFromParents = parents.flatMap { it.allFields }.distinct()
        val fieldsFromImplementation = implementation.allFields
        (fieldsFromImplementation - allFields).filter { it in fieldsFromParents }
    }

    override val packageName: String = implementation.packageName.replace(".impl", ".builder")
    var isOpen: Boolean = false
    var wantsCopy: Boolean = false
}

class IntermediateBuilder<BuilderField, Element>(
    override val typeName: String,
    override var packageName: String,
) : Builder<BuilderField, Element>()
        where BuilderField : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {
    val fields: MutableList<BuilderField> = mutableListOf()
    var materializedElement: Element? = null

    override val allFields: List<BuilderField> by lazy {
        mutableSetOf<BuilderField>().apply {
            parents.forEach { this += it.allFields }
            this += fields
        }.toList()
    }

    override val uselessFields: List<BuilderField> = emptyList()
}
