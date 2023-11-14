/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.ElementRef
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef

abstract class AbstractTreeBuilder {
    private val configurationCallbacks = mutableListOf<() -> Element>()

    abstract val rootElement: Element

    protected fun Field.skipInIrFactory() {
        useInIrFactoryStrategy = Field.UseFieldAsParameterInIrFactoryStrategy.No
    }

    protected fun Field.useFieldInIrFactory(defaultValue: String? = null) {
        useInIrFactoryStrategy = Field.UseFieldAsParameterInIrFactoryStrategy.Yes(defaultValue)
    }

    fun element(category: Element.Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementDelegate {
        val del = ElementDelegate(category, name)
        configurationCallbacks.add {
            del.element!!.apply {
                initializer()
                if (elementParents.isEmpty() && this != rootElement) {
                    elementParents.add(ElementRef(rootElement))
                }
            }
        }
        return del
    }

    protected fun Element.parent(type: ClassRef<*>) {
        otherParents.add(type)
    }

    protected fun Element.parent(type: ElementOrRef) {
        elementParents.add(ElementRef(type.element, type.args, type.nullable))
    }

    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }

    protected fun field(
        name: String,
        type: TypeRefWithNullability?,
        nullable: Boolean = false,
        mutable: Boolean = true,
        isChild: Boolean = false,
        initializer: SingleField.() -> Unit = {}
    ): SingleField {
        checkChildType(isChild, type, name)
        return SingleField(name, type?.copy(nullable) ?: InferredOverriddenType, mutable, isChild).apply(initializer)
    }

    protected fun listField(
        name: String,
        elementType: TypeRef?,
        nullable: Boolean = false,
        mutability: ListField.Mutability = ListField.Mutability.Immutable,
        isChild: Boolean = false,
        initializer: ListField.() -> Unit = {}
    ): ListField {
        checkChildType(isChild, elementType, name)
        val listType = when (mutability) {
            ListField.Mutability.List -> StandardTypes.mutableList
            ListField.Mutability.Array -> StandardTypes.array
            else -> StandardTypes.list
        }
        return ListField(
            name = name,
            elementType = elementType ?: InferredOverriddenType,
            listType = listType,
            isNullable = nullable,
            mutable = mutability == ListField.Mutability.Var,
            isChild = isChild,
            transformable = mutability != ListField.Mutability.Immutable,
        ).apply(initializer)
    }

    private fun checkChildType(isChild: Boolean, type: TypeRef?, name: String) {
        if (isChild) {
            require(type == null || type is GenericElementOrRef<*, *>) {
                "Field $name is a child field but has non-element type $type"
            }
        }
    }

    fun build(): Model {
        val elements = configurationCallbacks.map { it() }
        return Model(elements, rootElement)
    }

    companion object {
        val int = type<Int>()
        val string = type<String>()
        val boolean = type<Boolean>()
    }
}

class ElementDelegate(
    private val category: Element.Category,
    private val name: String?
) : ReadOnlyProperty<AbstractTreeBuilder, Element>, PropertyDelegateProvider<AbstractTreeBuilder, ElementDelegate> {
    var element: Element? = null
        private set

    override fun getValue(thisRef: AbstractTreeBuilder, property: KProperty<*>): Element {
        return element!!
    }

    override fun provideDelegate(thisRef: AbstractTreeBuilder, property: KProperty<*>): ElementDelegate {
        val path = thisRef.javaClass.name + "." + property.name
        element = Element(name ?: property.name.replaceFirstChar(Char::uppercaseChar), path, category)
        return this
    }
}
