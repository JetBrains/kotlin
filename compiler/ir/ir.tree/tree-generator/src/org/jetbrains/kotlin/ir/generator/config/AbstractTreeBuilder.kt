/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.config.AbstractElementConfigurator
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SingleField

abstract class AbstractTreeBuilder : AbstractElementConfigurator<Element, Field, Element.Category>() {

    override fun createElement(name: String, propertyName: String, category: Element.Category): Element {
        return Element(name, propertyName, category)
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

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = true,
        kind: AbstractField.Kind? = null,
        initializer: SingleField.() -> Unit = {},
    ): SingleField {
        return SingleField(
            name = name,
            typeRef = type.copy(nullable),
            mutable = mutable,
            kind = kind ?: getDefaultFieldKind(type)
        ).apply(initializer)
    }

    protected fun listField(
        name: String,
        baseType: TypeRef,
        nullable: Boolean = false,
        mutability: ListField.Mutability,
        kind: AbstractField.Kind? = null,
        initializer: ListField.() -> Unit = {},
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
            kind = kind ?: getDefaultFieldKind(baseType),
        ).apply(initializer)
    }

    /**
     * Constructs a field that represents the element's own symbol, i.e., for which
     * `element.symbol.owner === element` is always true.
     */
    protected fun declaredSymbol(type: TypeRefWithNullability) =
        field("symbol", type, mutable = false, kind = AbstractField.Kind.DeclaredSymbol)

    companion object {
        val int = type<Int>()
        val string = type<String>()
        val boolean = type<Boolean>()
    }
}
