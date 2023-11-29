/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.model

import org.jetbrains.kotlin.bir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ListField
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.topologicalSort
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

internal object InferredOverriddenType : TypeRefWithNullability {

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        renderingIsNotSupported()
    }

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override val nullable: Boolean
        get() = false

    override fun copy(nullable: Boolean) = this
}

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

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
    val fieldFakeOverrides = mutableMapOf<Field, FieldFakeOverride>()

    override val allFields: List<Field>
        get() = fields.toList()

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    override val parentInVisitor: Element?
        get() = error("No visitors")

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

    override val typeName = "Bir$name"
    val elementImplName = type(packageName + ".impl", typeName + "Impl")

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

    override var walkableChildren: List<Field> = emptyList()
    override val hasAcceptChildrenMethod: Boolean
        get() = walkableChildren.isNotEmpty()

    override var childrenOrderOverride: List<String>? = null

    override var visitorParameterName = category.defaultVisitorParam

    override val hasAcceptMethod: Boolean
        get() = false


    var generationCallback: (context(ImportCollector) SmartPrinter.() -> Unit)? = null

    override var kDoc: String? = null

    val usedTypes = mutableListOf<Importable>()

    var ownerSymbolType: TypeRef? = null

    override val hasTransformMethod get() = false
    override val transformableChildren: List<Field> get() = emptyList()
    override val hasTransformChildrenMethod: Boolean get() = false

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
) : AbstractField<Field>() {
    var isOverride = false

    var passViaConstructorParameter = false
    var isReadWriteTrackedProperty = false
    var initializeToThis = false

    val backingFieldName: String
        get() = "_$name"

    override fun toString() = "$name: $typeRef"

    override val isVolatile: Boolean
        get() = false

    override val isFinal: Boolean
        get() = false

    override val isLateinit: Boolean
        get() = false

    override val isParameter: Boolean
        get() = false

    override fun copy() = internalCopy().also(::updateFieldsInCopy)

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
    }

    protected abstract fun internalCopy(): Field
}

class SingleField(
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
    isChild: Boolean,
) : Field(name, mutable, isChild) {
    var getter: String? = null

    override fun replaceType(newType: TypeRefWithNullability) =
        SingleField(name, newType, isMutable, isChild).also(::updateFieldsInCopy)

    override fun internalCopy() = SingleField(name, typeRef, isMutable, isChild)
}

class ListField(
    name: String,
    override var baseType: TypeRef,
    private val isNullable: Boolean,
    override val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
    isChild: Boolean,
) : Field(name, mutable, isChild), ListField {

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType).copy(isNullable)

    override fun replaceType(newType: TypeRefWithNullability) = copy()

    override fun internalCopy() = ListField(name, baseType, isNullable, listType, isMutable, isChild)

    enum class Mutability {
        Immutable,
        Var,
        List,
        Array
    }
}

class FieldFakeOverride(
    val field: Field
) {
    var propertyId = -1
}