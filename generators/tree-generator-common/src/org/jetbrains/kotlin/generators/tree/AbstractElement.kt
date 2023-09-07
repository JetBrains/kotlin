/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A common interface representing a FIR or IR tree element.
 */
interface AbstractElement<Element : AbstractElement<Element, Field>, Field : AbstractField> : FieldContainer, ImplementationKindOwner {

    val name: String

    val fields: Set<Field>

    val parents: List<Element>

    val typeArguments: List<TypeArgument>

    val parentsArguments: Map<Element, Map<Importable, Importable>>

    val overridenFields: Map<Field, Map<Importable, Boolean>>

    val isSealed: Boolean
        get() = false

    override val allParents: List<ImplementationKindOwner>
        get() = parents

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics
}

val AbstractElement<*, *>.generics: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""