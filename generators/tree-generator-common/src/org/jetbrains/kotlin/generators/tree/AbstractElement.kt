/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.printer.generics

/**
 * A class representing a FIR or IR tree element.
 */
abstract class AbstractElement<Element, Field> : ElementOrRef<Element, Field>, FieldContainer, ImplementationKindOwner
        where Element : AbstractElement<Element, Field>,
              Field : AbstractField {

    abstract val name: String

    abstract val fields: Set<Field>

    abstract val params: List<TypeVariable>

    abstract val parentRefs: List<ElementOrRef<Element, Field>>

    open val isSealed: Boolean
        get() = false

    override val allParents: List<Element>
        get() = parentRefs.map { it.element }

    final override fun getTypeWithArguments(notNull: Boolean): String = type + generics

    abstract override val allFields: List<Field>

    final override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun copy(nullable: Boolean) =
        ElementRef(this as Element, args, nullable)

    @Suppress("UNCHECKED_CAST")
    final override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) =
        ElementRef(this as Element, args, nullable)
}
