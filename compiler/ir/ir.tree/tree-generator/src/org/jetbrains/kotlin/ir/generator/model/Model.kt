/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.config.FieldConfig
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
    override var elementParents: List<ElementRef> = emptyList()
    override var otherParents: MutableList<ClassRef<*>> = mutableListOf()

    override val allFields: List<Field>
        get() = fields.toList()

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    override var parentInVisitor: Element? = null
    var transformerReturnType: Element? = null

    override var kind: ImplementationKind? = when (config.typeKind) {
        TypeKind.Class -> ImplementationKind.AbstractClass
        TypeKind.Interface -> ImplementationKind.Interface
        null -> null
    }

    override val typeName
        get() = elementName2typeName(name)

    var isLeaf = config.isForcedLeaf
    val childrenOrderOverride: List<String>? = config.childrenOrderOverride
    override var walkableChildren: List<Field> = emptyList()
    override val transformableChildren get() = walkableChildren.filter { it.transformable }

    override val visitFunctionName = "visit" + (config.visitorName ?: name).replaceFirstChar(Char::uppercaseChar)
    override val visitorParameterName = config.visitorParam ?: config.category.defaultVisitorParam

    override var hasAcceptMethod = config.accept

    override val hasTransformMethod = config.transform

    override val hasAcceptChildrenMethod: Boolean
        get() = ownsChildren && (isRootElement || walkableChildren.isNotEmpty())

    override val hasTransformChildrenMethod: Boolean
        get() = ownsChildren && (isRootElement || transformableChildren.isNotEmpty())

    val transformByChildren = config.transformByChildren
    val ownsChildren = config.ownsChildren
    val generateIrFactoryMethod = config.generateIrFactoryMethod
    val fieldsToSkipInIrFactoryMethod = config.fieldsToSkipInIrFactoryMethod

    val generationCallback = config.generationCallback
    override val propertyName = config.propertyName

    override val kDoc = config.kDoc

    val usedTypes: List<Importable> = config.usedTypes

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

@Suppress("LeakingThis")
sealed class Field(
    config: FieldConfig,
    override val name: String,
    override var isMutable: Boolean,
    val isChild: Boolean,
) : AbstractField() {
    abstract val baseDefaultValue: String?
    abstract val baseGetter: String?
    abstract val transformable: Boolean

    val useInIrFactoryStrategy = config.useFieldInIrFactoryStrategy

    init {
        kDoc = config.kDoc
        optInAnnotation = config.optInAnnotation
        deprecation = config.deprecation
        visibility = config.visibility
    }

    override val withGetter: Boolean
        get() = baseGetter != null

    override val defaultValueInImplementation: String?
        get() = baseGetter ?: baseDefaultValue

    override val customSetter: String? = config.customSetter

    override fun toString() = "$name: $typeRef"

    override val isVolatile: Boolean
        get() = false

    override val isFinal: Boolean
        get() = defaultValueInImplementation != null

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
    override val baseDefaultValue: String?,
    override val baseGetter: String?,
) : Field(config, name, mutable, isChild) {
    override val transformable: Boolean
        get() = isMutable
}

class ListField(
    config: FieldConfig,
    name: String,
    var elementType: TypeRef,
    private val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
    isChild: Boolean,
    override val transformable: Boolean,
    override val baseDefaultValue: String?,
    override val baseGetter: String?,
) : Field(config, name, mutable, isChild) {
    override val typeRef: TypeRefWithNullability
        get() = listType.withArgs(elementType)
}
