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
import org.jetbrains.kotlin.ir.generator.model.SimpleField

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
        isChild: Boolean = true,
        initializer: SimpleField.() -> Unit = {}
    ): SimpleField {
        return SimpleField(name, type.copy(nullable), mutable, isChild).apply {
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
    ): ListField = ListField(
        name = name,
        baseType = baseType,
        isNullable = nullable,
        mutability = mutability,
        isChild = isChild,
    ).apply(initializer)

    /**
     * Constructs a field that represents the element's own symbol, i.e., for which
     * `element.symbol.owner === element` is always true.
     */
    protected fun declaredSymbol(type: TypeRefWithNullability) =
        field("symbol", type, mutable = false) {
            symbolFieldRole = AbstractField.SymbolFieldRole.DECLARED
        }

    /**
     * Constructs a field that represents a symbol that the element references but not owns.
     */
    protected fun referencedSymbol(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = true,
        initializer: SimpleField.() -> Unit = {},
    ) = field(
        name = name,
        type = type,
        nullable = nullable,
        mutable = mutable,
    ) {
        symbolFieldRole = AbstractField.SymbolFieldRole.REFERENCED
        initializer()
    }

    /**
     * Constructs a field that represents a symbol that the element references but not owns.
     */
    protected fun referencedSymbol(
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = true,
        initializer: SimpleField.() -> Unit = {},
    ) = referencedSymbol("symbol", type, nullable, mutable, initializer = initializer)

    /**
     * Constructs a field that represents a list of symbols that the element references but not owns.
     */
    protected fun referencedSymbolList(
        name: String,
        baseType: TypeRefWithNullability,
        nullable: Boolean = false,
        mutability: ListField.Mutability = ListField.Mutability.Var,
        initializer: ListField.() -> Unit = {},
    ) = listField(
        name = name,
        baseType = baseType,
        nullable = nullable,
        mutability = mutability,
    ) {
        symbolFieldRole = AbstractField.SymbolFieldRole.REFERENCED
        initializer()
    }
}
