/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ListField as AbstractListField
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.elementBaseType
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(
    name: String,
    override val propertyName: String,
    category: Category,
) : AbstractElement<Element, Field, Nothing>(name) {

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

    override val element: Element
        get() = this

    override val nullable: Boolean
        get() = false

    override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    /**
     * Allows to forcibly skip generation of the method for this element in visitors.
     */
    var generateVisitorMethod = true

    override val parentInVisitor: Element?
        get() {
            if (!generateVisitorMethod) return null
            return customParentInVisitor
                ?: elementParents.singleOrNull { it.typeKind == TypeKind.Class }?.element
                ?: IrTree.rootElement.takeIf { elementBaseType in otherParents }
        }


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

    override val namePrefix: String
        get() = "Ir"

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

    override var childrenOrderOverride: List<String>? = null

    override var visitorParameterName = category.defaultVisitorParam

    var customHasAcceptMethod: Boolean? = null

    override val hasAcceptMethod: Boolean
        get() = customHasAcceptMethod ?: (isLeaf && parentInVisitor != null)


    override var hasTransformMethod = false

    override val hasAcceptChildrenMethod: Boolean
        get() = hasAcceptOrTransformChildrenMethod(Element::walkableChildren)

    override val hasTransformChildrenMethod: Boolean
        get() = hasAcceptOrTransformChildrenMethod(Element::transformableChildren)

    private fun hasAcceptOrTransformChildrenMethod(walkableOrTransformableChildren: Element.() -> List<Field>): Boolean {
        if (!ownsChildren) return false
        if (!isRootElement && walkableOrTransformableChildren().isEmpty()) return false
        val atLeastOneParentHasAcceptOrTransformChildrenMethod = traverseParentsUntil { parent ->
            parent != this && parent.hasAcceptOrTransformChildrenMethod(walkableOrTransformableChildren) && !parent.isRootElement
        }
        return !atLeastOneParentHasAcceptOrTransformChildrenMethod
    }

    var transformByChildren = false
    var ownsChildren = true // If false, acceptChildren/transformChildren will NOT be generated.

    var generationCallback: (context(ImportCollector) SmartPrinter.() -> Unit)? = null

    override var kDoc: String? = null

    override fun toString() = name

    operator fun TypeVariable.unaryPlus() = apply {
        params.add(this)
    }

    operator fun Field.unaryPlus() = apply {
        fields.add(this)
    }
}

typealias ElementRef = GenericElementRef<Element>
typealias ElementOrRef = GenericElementOrRef<Element>

sealed class Field(
    override val name: String,
    override var isMutable: Boolean,
) : AbstractField<Field>() {
    var baseDefaultValue: String? = null
    var baseGetter: String? = null

    sealed class UseFieldAsParameterInIrFactoryStrategy {

        data object No : UseFieldAsParameterInIrFactoryStrategy()

        data class Yes(val defaultValue: String?) : UseFieldAsParameterInIrFactoryStrategy()
    }

    var customUseInIrFactoryStrategy: UseFieldAsParameterInIrFactoryStrategy? = null

    val useInIrFactoryStrategy: UseFieldAsParameterInIrFactoryStrategy
        get() = customUseInIrFactoryStrategy
            ?: if (isChild && containsElement) {
                UseFieldAsParameterInIrFactoryStrategy.No
            } else {
                UseFieldAsParameterInIrFactoryStrategy.Yes(null)
            }

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

    override val isParameter: Boolean
        get() = false

    override fun copy() = internalCopy().also(::updateFieldsInCopy)

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.baseDefaultValue = baseDefaultValue
        copy.baseGetter = baseGetter
        copy.customUseInIrFactoryStrategy = customUseInIrFactoryStrategy
        copy.customSetter = customSetter
    }

    protected abstract fun internalCopy(): Field
}

class SingleField(
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
) : Field(name, mutable) {

    override fun replaceType(newType: TypeRefWithNullability) =
        SingleField(name, newType, isMutable).also(::updateFieldsInCopy)

    override fun internalCopy() = SingleField(name, typeRef, isMutable)
}

class ListField(
    name: String,
    override var baseType: TypeRef,
    private val isNullable: Boolean,
    override val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
) : Field(name, mutable), AbstractListField {

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType).copy(isNullable)

    override fun replaceType(newType: TypeRefWithNullability) = copy()

    override fun internalCopy() = ListField(name, baseType, isNullable, listType, isMutable)

    enum class Mutability {
        Var,
        MutableList,
        Array
    }
}
