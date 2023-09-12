/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.printer.generics

/**
 * A common interface representing a FIR or IR tree element.
 */
interface AbstractElement<Element : AbstractElement<Element, Field>, Field : AbstractField> : FieldContainer, ImplementationKindOwner,
    TypeRef /* TODO: Replace with ElementOrRef */ {

    val name: String

    val fields: Set<Field>

    val parents: List<Element>

    val params: List<TypeVariable>

    // TODO: Remove this property as soon as we replace org.jetbrains.kotlin.fir.tree.generator.model.ElementWithArguments with
    // a generic ElementRef class
    val typeArguments: List<TypeArgument>

    val parentsArguments: Map<Element, Map<TypeRef, TypeRef>>

    val overridenFields: Map<Field, Map<Field, Boolean>>

    val isSealed: Boolean
        get() = false

    override val allParents: List<ImplementationKindOwner>
        get() = parents

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics
}
