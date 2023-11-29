/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.ElementOrRef
import org.jetbrains.kotlin.ir.generator.model.ElementRef
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractTreeBuilder {
    private val configurationCallbacks = mutableListOf<() -> Element>()

    abstract val rootElement: Element

    protected fun Field.skipInIrFactory() {
        customUseInIrFactoryStrategy = Field.UseFieldAsParameterInIrFactoryStrategy.No
    }

    protected fun Field.useFieldInIrFactory(defaultValue: String? = null) {
        customUseInIrFactoryStrategy = Field.UseFieldAsParameterInIrFactoryStrategy.Yes(defaultValue)
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

    protected fun Element.needAcceptMethod() {
        customHasAcceptMethod = true
    }

    protected fun Element.noAcceptMethod() {
        customHasAcceptMethod = false
    }

    protected fun Element.needTransformMethod() {
        hasTransformMethod = true
    }

    protected fun Element.noMethodInVisitor() {
        generateVisitorMethod = false
    }

    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = true,
        isChild: Boolean = true,
        initializer: SingleField.() -> Unit = {}
    ): SingleField {
        return SingleField(name, type.copy(nullable), mutable).apply {
            this.isChild = isChild
            initializer()
        }
    }

    protected fun listField(
        name: String,
        baseType: TypeRef,
        nullable: Boolean = false,
        mutability: ListField.Mutability,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {}
    ): ListField {
        val listType = when (mutability) {
            ListField.Mutability.MutableList -> StandardTypes.mutableList
            ListField.Mutability.Array -> StandardTypes.array
            else -> StandardTypes.list
        }
        return ListField(
            name = name,
            baseType = baseType,
            listType = listType,
            isNullable = nullable,
            mutable = mutability == ListField.Mutability.Var,
        ).apply(initializer).apply {
            this.isChild = isChild
            initializer()
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
