/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.config

import org.jetbrains.kotlin.bir.generator.BirTree
import org.jetbrains.kotlin.bir.generator.childElementList
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.ElementOrRef
import org.jetbrains.kotlin.bir.generator.model.InferredOverriddenType
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef

abstract class AbstractTreeBuilder {
    private val configurationCallbacks = mutableListOf<() -> Element>()

    abstract val rootElement: Element

    fun element(category: Element.Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementConfigDel {
        val del = ElementConfigDel(category, name)
        configurationCallbacks.add {
            del.element!!.apply {
                if (this != BirTree.rootElement) {
                    parent(BirTree.rootElement)
                }
                initializer()
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
        isChild: Boolean = true,
        initializer: SingleField.() -> Unit = {},
    ): SingleField {
        val isChildElement = type is GenericElementOrRef<*> && isChild
        return SingleField(name, type?.copy(nullable) ?: InferredOverriddenType, mutable, isChildElement).apply(initializer)
    }

    protected fun listField(
        name: String,
        elementType: TypeRef?,
        nullable: Boolean = false,
        mutability: ListField.Mutability = ListField.Mutability.Immutable,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {},
    ): ListField {
        val isChildElement = elementType is GenericElementOrRef<*> && isChild

        val listType = when {
            isChildElement -> childElementList
            mutability == ListField.Mutability.List -> StandardTypes.mutableList
            mutability == ListField.Mutability.Array -> StandardTypes.array
            else -> StandardTypes.list
        }
        return ListField(
            name = name,
            baseType = elementType ?: InferredOverriddenType,
            listType = listType,
            isNullable = nullable,
            mutable = mutability == ListField.Mutability.Var && !isChildElement,
            isChild = isChildElement,
        ).apply(initializer)
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

class ElementConfigDel(
    private val category: Element.Category,
    private val name: String?,
) : ReadOnlyProperty<AbstractTreeBuilder, Element>, PropertyDelegateProvider<AbstractTreeBuilder, ElementConfigDel> {
    var element: Element? = null
        private set

    override fun getValue(thisRef: AbstractTreeBuilder, property: KProperty<*>): Element {
        return element!!
    }

    override fun provideDelegate(thisRef: AbstractTreeBuilder, property: KProperty<*>): ElementConfigDel {
        val path = thisRef.javaClass.name + "." + property.name
        element = Element(name ?: property.name.replaceFirstChar(Char::uppercaseChar), path, category)
        return this
    }
}
