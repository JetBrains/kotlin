/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractElementConfigurator<Element, Field, Category>
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {

    inner class ElementDelegate(
        private val category: Category,
        private val name: String?,
        private val isSealed: Boolean,
    ) : ReadOnlyProperty<AbstractElementConfigurator<Element, Field, Category>, Element>,
        PropertyDelegateProvider<AbstractElementConfigurator<Element, Field, Category>, ElementDelegate> {

        var element: Element? = null
            private set

        override fun getValue(thisRef: AbstractElementConfigurator<Element, Field, Category>, property: KProperty<*>): Element {
            return element!!
        }

        override fun provideDelegate(
            thisRef: AbstractElementConfigurator<Element, Field, Category>,
            property: KProperty<*>,
        ): ElementDelegate {
            val path = thisRef.javaClass.name + "." + property.name
            element = createElement(name ?: property.name.replaceFirstChar(Char::uppercaseChar), path, category).also {
                it.isSealed = isSealed
            }
            return this
        }
    }

    protected abstract fun createElement(name: String, propertyName: String, category: Category): Element

    private val configurationCallbacks = mutableListOf<() -> Element>()

    abstract val rootElement: Element

    fun build(): Model<Element> {
        val elements = configurationCallbacks.map { it() }
        return Model(elements, rootElement)
    }

    private fun createElement(
        category: Category,
        name: String? = null,
        isSealed: Boolean,
        initializer: Element.() -> Unit = {},
    ): ElementDelegate {
        val del = ElementDelegate(category, name, isSealed)
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

    fun element(category: Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementDelegate =
        createElement(category, name, isSealed = false, initializer)

    fun sealedElement(category: Category, name: String? = null, initializer: Element.() -> Unit = {}): ElementDelegate =
        createElement(category, name, isSealed = true, initializer)

    protected fun Element.parent(type: ClassRef<*>) {
        otherParents.add(type)
    }

    protected fun Element.parent(type: ElementOrRef<Element>) {
        elementParents.add(ElementRef(type.element, type.args, type.nullable))
    }

    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }
}

fun <Element, Field> AbstractElementConfigurator<Element, Field, Nothing?>.element(
    name: String? = null,
    initializer: Element.() -> Unit = {},
): AbstractElementConfigurator<Element, Field, Nothing?>.ElementDelegate
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {
    return element(null, name, initializer)
}

fun <Element, Field> AbstractElementConfigurator<Element, Field, Nothing?>.sealedElement(
    name: String? = null,
    initializer: Element.() -> Unit = {},
): AbstractElementConfigurator<Element, Field, Nothing?>.ElementDelegate
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {
    return sealedElement(null, name, initializer)
}
