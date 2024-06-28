/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A field that is used to store a list with arbitrary elements.
 */
interface ListField {

    /**
     * The element type of the list.
     */
    val baseType: TypeRef

    /**
     * The list type of the field, e.g. [List] or [MutableList].
     */
    val listType: ClassRef<PositionTypeParameterRef>

    val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType)
}