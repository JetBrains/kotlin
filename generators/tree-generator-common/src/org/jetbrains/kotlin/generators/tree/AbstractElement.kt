/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A common interface representing a FIR or IR tree element.
 */
interface AbstractElement<Element : AbstractElement<Element, Field>, Field : AbstractField> : FieldContainer, ImplementationKindOwner,
    TypeRef /* TODO: Replace with ElementOrRef */ {

    val name: String

    val fields: Set<Field>

    val parents: List<Element>

    val params: List<TypeVariable> // TODO: Rename to `typeParameters` (rn this name would clash with the extension function)

    val typeArguments: List<TypeArgument>

    val parentsArguments: Map<Element, Map<TypeRef, TypeRef>>

    val overridenFields: Map<Field, Map<Field, Boolean>>

    val isSealed: Boolean
        get() = false

    override val allParents: List<ImplementationKindOwner>
        get() = parents

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics

    override val allFields: List<Field>
}

val AbstractElement<*, *>.generics: String
    get() = params.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""

fun AbstractElement<*, *>.typeParameters(end: String = ""): String = params.takeIf { it.isNotEmpty() }
    ?.joinToString(", ", "<", ">$end") { param ->
        param.name + (param.bounds.singleOrNull()?.let { " : ${it.typeWithArguments}" } ?: "")
    } ?: ""
