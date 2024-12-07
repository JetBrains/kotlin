/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.imports.Importable

sealed class Builder<ElementField, Element> : FieldContainer<ElementField>, TypeRefWithNullability, Importable
        where ElementField : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {

    val parents: MutableList<IntermediateBuilder<ElementField, Element>> = mutableListOf()

    val usedTypes: MutableList<Importable> = mutableListOf()

    abstract val uselessFields: List<ElementField>

    private val fieldsFromParentIndex: Map<String, Boolean> by lazy {
        mutableMapOf<String, Boolean>().apply {
            for (field in allFields + uselessFields) {
                this[field.name] = parents.any { field.name in it.allFields.map { it.name } }
            }
        }
    }

    fun isFromParent(field: AbstractField<*>): Boolean = fieldsFromParentIndex.getValue(field.name)

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(typeName)
    }

    override val nullable: Boolean
        get() = false

    override fun copy(nullable: Boolean) = this
}

class LeafBuilder<Field, Element, Implementation>(
    val implementation: Implementation,
) : Builder<Field, Element>()
        where Field : AbstractField<Field>,
              Element : AbstractElement<Element, Field, Implementation>,
              Implementation : AbstractImplementation<Implementation, Element, Field> {
    override val typeName: String
        get() = (implementation.name ?: implementation.element.typeName) + "Builder"

    override val allFields: List<Field> by lazy { implementation.fieldsInConstructor }

    override val uselessFields: List<Field> by lazy {
        val fieldsFromParents = parents.flatMap { it.allFields }.map { it.name }.toSet()
        val fieldsFromImplementation = implementation.allFields
        (fieldsFromImplementation - allFields).filter { it.name in fieldsFromParents }
    }

    override val packageName: String = implementation.packageName.replace(".impl", ".builder")
    var isOpen: Boolean = false
    var wantsCopy: Boolean = false
}

class IntermediateBuilder<Field, Element>(
    override val typeName: String,
    override var packageName: String,
) : Builder<Field, Element>()
        where Field : AbstractField<*>,
              Element : AbstractElement<Element, *, *> {
    val fields: MutableList<Field> = mutableListOf()
    var materializedElement: Element? = null
    var isSealed: Boolean = false

    override val allFields: List<Field> by lazy {
        buildMap<String, Field> {
            parents.forEach { parent ->
                parent.allFields.associateByTo(this) { it.name }
            }
            fields.associateByTo(this) { it.name }
        }.values.toList()
    }

    override val uselessFields: List<Field> = emptyList()
}
