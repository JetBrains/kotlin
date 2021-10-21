/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import org.jetbrains.kotlin.ir.generator.util.TypeRef
import org.jetbrains.kotlin.ir.generator.util.TypeVariable
import org.jetbrains.kotlin.ir.generator.util.type
import org.jetbrains.kotlin.types.Variance
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractTreeBuilder {
    private val configurationCallbacks = mutableListOf<() -> ElementConfig>()

    abstract val rootElement: ElementConfig

    fun element(category: ElementConfig.Category, name: String? = null, initializer: ElementConfig.() -> Unit = {}): ElementConfigDel {
        val del = ElementConfigDel(category, name)
        configurationCallbacks.add {
            del.element!!.apply { initializer() }
        }
        return del
    }

    protected fun ElementConfig.parent(type: TypeRef) {
        parents.add(type)
    }

    protected fun param(name: String, vararg bounds: TypeRef, variance: Variance = Variance.INVARIANT): TypeVariable {
        return TypeVariable(name, bounds.toList(), variance)
    }

    protected fun field(
        name: String,
        type: TypeRef?,
        nullable: Boolean = false,
        mutable: Boolean = false,
        isChild: Boolean = false,
        initializer: SimpleFieldConfig.() -> Unit = {}
    ): SimpleFieldConfig {
        checkChildType(isChild, type, name)
        return SimpleFieldConfig(name, type, nullable, mutable, isChild).apply(initializer)
    }

    protected fun listField(
        name: String,
        elementType: TypeRef?,
        nullable: Boolean = false,
        mutability: ListFieldConfig.Mutability = ListFieldConfig.Mutability.Immutable,
        isChild: Boolean = false,
        initializer: ListFieldConfig.() -> Unit = {}
    ): ListFieldConfig {
        checkChildType(isChild, elementType, name)
        return ListFieldConfig(name, elementType, nullable, mutability, isChild).apply(initializer)
    }

    private fun checkChildType(isChild: Boolean, type: TypeRef?, name: String) {
        if (isChild) {
            require(type == null || type is ElementConfigOrRef) { "Field $name is a child field but has non-element type $type" }
        }
    }

    fun build(): Config {
        val elements = configurationCallbacks.map { it() }
        return Config(elements, rootElement)
    }

    companion object {
        val int = type<Int>()
        val string = type<String>()
        val boolean = type<Boolean>()
    }
}

class ElementConfigDel(
    private val category: ElementConfig.Category,
    private val name: String?
) : ReadOnlyProperty<AbstractTreeBuilder, ElementConfig>, PropertyDelegateProvider<AbstractTreeBuilder, ElementConfigDel> {
    var element: ElementConfig? = null
        private set

    override fun getValue(thisRef: AbstractTreeBuilder, property: KProperty<*>): ElementConfig {
        return element!!
    }

    override fun provideDelegate(thisRef: AbstractTreeBuilder, property: KProperty<*>): ElementConfigDel {
        val path = thisRef.javaClass.name + "." + property.name
        element = ElementConfig(path, name ?: property.name, category)
        return this
    }
}
