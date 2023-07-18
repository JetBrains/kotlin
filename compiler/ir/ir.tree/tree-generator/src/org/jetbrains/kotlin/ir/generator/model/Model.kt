/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import com.squareup.kotlinpoet.CodeBlock
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.config.FieldConfig
import org.jetbrains.kotlin.ir.generator.util.*
import org.jetbrains.kotlin.utils.topologicalSort

class Element(
    config: ElementConfig,
    val name: String,
    val packageName: String,
    val params: List<TypeVariable>,
    val fields: MutableList<Field>,
    val additionalFactoryMethodParameters: MutableList<Field>,
) {
    var elementParents: List<ElementRef> = emptyList()
    var otherParents: List<ClassRef<*>> = emptyList()
    var visitorParent: ElementRef? = null
    var transformerReturnType: Element? = null
    val targetKind = config.typeKind
    var kind: Kind? = null
    val typeName
        get() = elementName2typeName(name)
    val allParents: List<ClassOrElementRef>
        get() = elementParents + otherParents
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
    val suppressPrint = config.suppressPrint
    val propertyName = config.propertyName
    val kDoc = config.kDoc
    val additionalImports: List<Import> = config.additionalImports

    override fun toString() = name

    enum class Kind(val typeKind: TypeKind) {
        FinalClass(TypeKind.Class),
        OpenClass(TypeKind.Class),
        AbstractClass(TypeKind.Class),
        SealedClass(TypeKind.Class),
        Interface(TypeKind.Interface),
        SealedInterface(TypeKind.Interface),
    }

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

data class ElementRef(
    val element: Element,
    override val args: Map<NamedTypeParameterRef, TypeRef> = emptyMap(),
    override val nullable: Boolean = false,
) : ParametrizedTypeRef<ElementRef, NamedTypeParameterRef>, ClassOrElementRef {
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)
    override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)
    override fun toString() = "${element.name}<${args}>"
}

sealed class Field(
    config: FieldConfig,
    val name: String,
    val nullable: Boolean,
    val mutable: Boolean,
    val isChild: Boolean,
) {
    abstract val type: TypeRef
    abstract val baseDefaultValue: CodeBlock?
    abstract val baseGetter: CodeBlock?
    var isOverride = false
    var needsDescriptorApiAnnotation = false
    abstract val transformable: Boolean

    val useInIrFactoryStrategy = config.useFieldInIrFactoryStrategy
    val kdoc = config.kdoc

    val printProperty = config.printProperty
    val generationCallback = config.generationCallback

    override fun toString() = "$name: $type"
}

class SingleField(
    config: FieldConfig,
    name: String,
    override var type: TypeRef,
    nullable: Boolean,
    mutable: Boolean,
    isChild: Boolean,
    override val baseDefaultValue: CodeBlock?,
    override val baseGetter: CodeBlock?,
) : Field(config, name, nullable, mutable, isChild) {
    override val transformable: Boolean
        get() = mutable
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
    override val type: TypeRef
        get() = listType.withArgs(elementType)
}
