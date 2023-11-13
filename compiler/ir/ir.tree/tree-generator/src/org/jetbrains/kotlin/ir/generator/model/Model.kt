/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.topologicalSort
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(
    name: String,
    override val propertyName: String,
    category: Category,
) : AbstractElement<Element, Field>(name) {

    enum class Category(private val packageDir: String, val defaultVisitorParam: String) {
        Expression("expressions", "expression"),
        Declaration("declarations", "declaration"),
        Other("", "element");

        val packageName: String get() = BASE_PACKAGE + if (packageDir.isNotEmpty()) ".$packageDir" else ""
    }

    override val packageName: String = category.packageName

    override val elementParents = mutableListOf<ElementRef>()
    override var otherParents: MutableList<ClassRef<*>> = mutableListOf()

    override val params = mutableListOf<TypeVariable>()

    override val fields = mutableSetOf<Field>()

    val additionalIrFactoryMethodParameters = mutableListOf<Field>()
    var generateIrFactoryMethod = category == Category.Declaration
    val fieldsToSkipInIrFactoryMethod = hashSetOf<String>()


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

    var typeKind: TypeKind? = null
        set(value) {
            kind = when (value) {
                TypeKind.Class -> ImplementationKind.AbstractClass
                TypeKind.Interface -> ImplementationKind.Interface
                null -> null
            }
            field = value
        }

    override var kind: ImplementationKind? = null

    override val typeName = "Ir$name"

    /**
     * Whether this element is semantically a leaf element in the hierarchy.
     *
     * This is set automatically by the [markLeaves] function for all true leaves, but can also be set manually to `true` if
     * this element should be considered a leaf semantically.
     *
     * For example, we only generate [org.jetbrains.kotlin.ir.declarations.IrFactory] methods for leaf elements.
     * If we want to generate a method for this element, but it has subclasses, it can be done by manually setting this property to `true`.
     */
    var isLeaf = false

    var childrenOrderOverride: List<String>? = null
    override var walkableChildren: List<Field> = emptyList()
    override val transformableChildren get() = walkableChildren.filter { it.transformable }

    override var visitorParameterName = category.defaultVisitorParam

    override var hasAcceptMethod = false // By default, accept is generated only for leaves.

    override var hasTransformMethod = false

    override val hasAcceptChildrenMethod: Boolean
        get() = ownsChildren && (isRootElement || walkableChildren.isNotEmpty())

    override val hasTransformChildrenMethod: Boolean
        get() = ownsChildren && (isRootElement || transformableChildren.isNotEmpty())

    var transformByChildren = false
    var ownsChildren = true // If false, acceptChildren/transformChildren will NOT be generated.

    var generationCallback: (context(ImportCollector) SmartPrinter.() -> Unit)? = null

    override var kDoc: String? = null

    val usedTypes = mutableListOf<Importable>()

    override fun toString() = name

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

    operator fun TypeVariable.unaryPlus() = apply {
        params.add(this)
    }

    operator fun Field.unaryPlus() = apply {
        fields.add(this)
    }
}

typealias ElementRef = GenericElementRef<Element, Field>
typealias ElementOrRef = GenericElementOrRef<Element, Field>

sealed class Field(
    override val name: String,
    override var isMutable: Boolean,
    val isChild: Boolean,
) : AbstractField() {
    var baseDefaultValue: String? = null
    var baseGetter: String? = null

    abstract val transformable: Boolean

    sealed class UseFieldAsParameterInIrFactoryStrategy {

        data object No : UseFieldAsParameterInIrFactoryStrategy()

        data class Yes(val defaultValue: String?) : UseFieldAsParameterInIrFactoryStrategy()
    }

    var useInIrFactoryStrategy =
        if (isChild) UseFieldAsParameterInIrFactoryStrategy.No else UseFieldAsParameterInIrFactoryStrategy.Yes(null)

    override val withGetter: Boolean
        get() = baseGetter != null

    override val defaultValueInImplementation: String?
        get() = baseGetter ?: baseDefaultValue

    override var customSetter: String? = null

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
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
    isChild: Boolean,
) : Field(name, mutable, isChild) {
    override val transformable: Boolean
        get() = isMutable
}

class ListField(
    name: String,
    var elementType: TypeRef,
    private val isNullable: Boolean,
    private val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
    isChild: Boolean,
    override val transformable: Boolean,
) : Field(name, mutable, isChild) {

    override val typeRef: TypeRefWithNullability
        get() = listType.withArgs(elementType).copy(isNullable)

    enum class Mutability {
        Immutable,
        Var,
        List,
        Array
    }
}
