/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import org.jetbrains.kotlin.bir.generator.config.ElementConfig
import org.jetbrains.kotlin.bir.generator.config.FieldConfig
import org.jetbrains.kotlin.bir.generator.config.SimpleFieldConfig
import org.jetbrains.kotlin.bir.generator.util.Import
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.topologicalSort
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(
    config: ElementConfig,
    override val name: String,
    override val packageName: String,
    override val params: List<TypeVariable>,
    override val fields: MutableSet<Field>,
) : AbstractElement<Element, Field>() {
    override var elementParents: List<ElementRef> = emptyList()
    override var otherParents: MutableList<ClassRef<*>> = mutableListOf()
    var visitorParent: ElementRef? = null
    var transformerReturnType: Element? = null
    val targetKind = config.typeKind
    override var kind: ImplementationKind? = null
    override val typeName
        get() = elementName2typeName(name)
    var isLeaf = config.isForcedLeaf
    var ownerSymbolType: TypeRef? = null
    val childrenOrderOverride: List<String>? = config.childrenOrderOverride
    override var allFields: List<Field> = emptyList()
    val allChildren get() = allFields.filter { it.isChild }

    override val walkableChildren: List<Field> get() = emptyList()
    override val transformableChildren: List<Field> get() = emptyList()

    val generationCallback = config.generationCallback
    override val propertyName = config.propertyName
    override val kDoc = config.kDoc
    val additionalImports: List<Import> = config.additionalImports

    val elementImplName = ClassName(packageName + ".impl", typeName + "Impl")
    val type: String
        get() = typeName

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    override fun toString() = name

    companion object {
        fun elementName2typeName(name: String) = "Bir" + name.replaceFirstChar(Char::uppercaseChar)
    }

    fun elementParentsRecursively(): List<ElementRef> {
        val linkedSet = buildSet {
            fun recurse(element: Element) {
                addAll(element.elementParents)
                element.elementParents.forEach { recurse(it.element) }
            }
            recurse(this@Element)
        }
        return topologicalSort(linkedSet) {
            element.elementParents
        }
    }

    fun allFieldsRecursively(): List<Field> {
        val parentFields = elementParentsRecursively()
            .reversed()
            .flatMap { it.element.fields }
        return (parentFields + fields)
            .asReversed()
            .distinctBy { it.name }
            .asReversed()
    }
}

typealias ElementRef = GenericElementRef<Element, Field>
typealias ElementOrRef = GenericElementOrRef<Element, Field>

sealed class Field(
    config: FieldConfig,
    override val name: String,
    override val isMutable: Boolean,
    val isChild: Boolean,
) : AbstractField() {
    abstract val baseDefaultValue: CodeBlock?
    var isOverride = false
    var needsDescriptorApiAnnotation = false

    var passViaConstructorParameter = false
    val initializeToThis = (config as? SimpleFieldConfig)?.initializeToThis ?: false

    val kdoc = config.kdoc
    val printProperty = config.printProperty
    val generationCallback = config.generationCallback

    override fun toString() = "$name: $typeRef"

    override val isVolatile: Boolean
        get() = false

    override val isFinal: Boolean
        get() = false

    override val isLateinit: Boolean
        get() = false

    override val isParameter: Boolean
        get() = false
}

class SingleField(
    config: FieldConfig,
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
    isChild: Boolean,
    override val baseDefaultValue: CodeBlock?,
) : Field(config, name, mutable, isChild) {
    val backingFieldName: String
        get() = "_$name"
}

class ListField(
    config: FieldConfig,
    name: String,
    var elementType: TypeRef,
    val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
    isChild: Boolean,
    override val baseDefaultValue: CodeBlock?,
) : Field(config, name, mutable, isChild) {
    override val typeRef: TypeRefWithNullability
        get() = listType.withArgs(elementType)
}
