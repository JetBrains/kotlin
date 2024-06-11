/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.model.ListField
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementOrRef
import org.jetbrains.kotlin.generators.tree.config.AbstractElementConfigurator

abstract class AbstractFirTreeBuilder() : AbstractElementConfigurator<Element, Field, Element.Kind>() {
    override fun createElement(name: String, propertyName: String, category: Element.Kind): Element {
        return Element(name, propertyName.replace(".NodeConfigurator", ".FirTreeBuilder"), category)
    }

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        isChild: Boolean = true,
        initializer: SingleField.() -> Unit = {},
    ): SingleField {
        val isMutable = type is ElementOrRef<*> || withReplace
        return SingleField(
            name,
            type.copy(nullable),
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            withTransform = withTransform
        ).apply(initializer)
    }

    protected fun field(
        type: ClassOrElementRef,
        nullable: Boolean = false,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        isChild: Boolean = true,
        initializer: SingleField.() -> Unit = {},
    ): SingleField {
        val name = when (type) {
            is ClassRef<*> -> type.simpleName
            is ElementOrRef<*> -> type.element.name
        }.replaceFirstChar(Char::lowercaseChar)
        return field(
            name,
            type = type,
            nullable = nullable,
            withReplace = withReplace,
            withTransform = withTransform,
            isChild = isChild,
            initializer = initializer,
        )
    }

    protected fun listField(
        name: String,
        baseType: TypeRef,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        useMutableOrEmpty: Boolean = false,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {},
    ): Field {
        return ListField(
            name,
            baseType,
            withReplace = withReplace,
            isChild = isChild,
            isMutableOrEmptyList = useMutableOrEmpty,
            withTransform = withTransform,
        ).apply(initializer)
    }

    protected fun listField(
        elementOrRef: ElementOrRef<*>,
        withReplace: Boolean = false,
        withTransform: Boolean = false,
        useMutableOrEmpty: Boolean = false,
        isChild: Boolean = true,
        initializer: ListField.() -> Unit = {},
    ): Field {
        val name = elementOrRef.element.name.replaceFirstChar(Char::lowercaseChar) + "s"
        return listField(
            name,
            elementOrRef,
            withReplace = withReplace,
            isChild = isChild,
            useMutableOrEmpty = useMutableOrEmpty,
            withTransform = withTransform,
            initializer = initializer,
        )
    }

    protected fun declaredSymbol(name: String, symbolType: ClassRef<*>): Field =
        field(name, symbolType)
            .apply {
                symbolFieldRole = AbstractField.SymbolFieldRole.DECLARED
                skippedInCopy = true
            }

    protected fun declaredSymbol(symbolType: ClassRef<*>): Field = declaredSymbol("symbol", symbolType)

    protected fun referencedSymbol(name: String, symbolType: ClassRef<*>, nullable: Boolean = false, withReplace: Boolean = false, initializer: SingleField.() -> Unit = {}): Field =
        field(name, symbolType, nullable, withReplace)
            .apply { symbolFieldRole = AbstractField.SymbolFieldRole.REFERENCED }
            .apply(initializer)

    protected fun referencedSymbol(symbolType: ClassRef<*>, nullable: Boolean = false, withReplace: Boolean = false, initializer: SingleField.() -> Unit = {}): Field =
        referencedSymbol("symbol", symbolType, nullable, withReplace, initializer)

    protected fun Element.generateBooleanFields(vararg names: String) {
        names.forEach {
            +field(
                if (it.startsWith("is") || it.startsWith("has")) it else "is${it.replaceFirstChar(Char::uppercaseChar)}",
                StandardTypes.boolean
            )
        }
    }

    protected fun Element.needTransformOtherChildren() {
        _needTransformOtherChildren = true
    }
}
