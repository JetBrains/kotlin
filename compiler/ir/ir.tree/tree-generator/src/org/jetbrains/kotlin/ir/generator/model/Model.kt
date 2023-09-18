/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import com.squareup.kotlinpoet.CodeBlock
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.config.FieldConfig
import org.jetbrains.kotlin.ir.generator.util.*
import org.jetbrains.kotlin.utils.topologicalSort
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(
    config: ElementConfig,
    override val name: String,
    override val packageName: String,
    override val params: List<TypeVariable>,
    override val fields: MutableSet<Field>,
    val additionalFactoryMethodParameters: MutableList<Field>,
) : AbstractElement<Element, Field>() {
    var elementParents: List<ElementRef> = emptyList()
    var otherParents: List<ClassRef<*>> = emptyList()

    override val parentRefs: List<ElementOrRef>
        get() = elementParents

    override val allFields: List<Field>
        get() = fields.toList()

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    var visitorParent: ElementRef? = null
    var transformerReturnType: Element? = null

    override var kind: ImplementationKind? = when (config.typeKind) {
        TypeKind.Class -> ImplementationKind.AbstractClass
        TypeKind.Interface -> ImplementationKind.Interface
        null -> null
    }

    val typeName
        get() = elementName2typeName(name)

    override val type: String
        get() = typeName

    var isLeaf = config.isForcedLeaf
    val childrenOrderOverride: List<String>? = config.childrenOrderOverride
    var walkableChildren: List<Field> = emptyList()
    val transformableChildren get() = walkableChildren.filter { it.transformable }

    val visitFunName = "visit" + (config.visitorName ?: name).replaceFirstChar(Char::uppercaseChar)
    val visitorParam = config.visitorParam ?: config.category.defaultVisitorParam
    var accept = config.accept
    val transform = config.transform
    val transformByChildren = config.transformByChildren
    val ownsChildren = config.ownsChildren
    val generateIrFactoryMethod = config.generateIrFactoryMethod
    val fieldsToSkipInIrFactoryMethod = config.fieldsToSkipInIrFactoryMethod

    val generationCallback = config.generationCallback
    val propertyName = config.propertyName
    val kDoc = config.kDoc
    val additionalImports: List<Import> = config.additionalImports

    override fun toString() = name

    companion object {
        fun elementName2typeName(name: String) = "Ir" + name.replaceFirstChar(Char::uppercaseChar)
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
    override val nullable: Boolean,
    override var isMutable: Boolean,
    val isChild: Boolean,
) : AbstractField() {
    abstract val baseDefaultValue: CodeBlock?
    abstract val baseGetter: CodeBlock?
    var isOverride = false
    var needsDescriptorApiAnnotation = false
    abstract val transformable: Boolean

    val useInIrFactoryStrategy = config.useFieldInIrFactoryStrategy
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

    override val type: String
        get() = typeRef.type
    override val packageName: String?
        get() = typeRef.packageName

    override fun getTypeWithArguments(notNull: Boolean): String = typeRef.getTypeWithArguments(notNull)
}

class SingleField(
    config: FieldConfig,
    name: String,
    override var typeRef: TypeRef,
    nullable: Boolean,
    mutable: Boolean,
    isChild: Boolean,
    override val baseDefaultValue: CodeBlock?,
    override val baseGetter: CodeBlock?,
) : Field(config, name, nullable, mutable, isChild) {
    override val transformable: Boolean
        get() = isMutable
}

class ListField(
    config: FieldConfig,
    name: String,
    var elementType: TypeRef,
    private val listType: ClassRef<PositionTypeParameterRef>,
    nullable: Boolean,
    mutable: Boolean,
    isChild: Boolean,
    override val transformable: Boolean,
    override val baseDefaultValue: CodeBlock?,
    override val baseGetter: CodeBlock?,
) : Field(config, name, nullable, mutable, isChild) {
    override val typeRef: TypeRef
        get() = listType.withArgs(elementType)
}
