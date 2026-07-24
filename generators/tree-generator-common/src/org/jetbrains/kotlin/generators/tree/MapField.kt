/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * A field that is used to store a map with arbitrary elements.
 */
interface MapField {

    /**
     * The key type of the map.
     */
    val keyType: TypeRef

    /**
     * The value type of the map.
     */
    val valueType: TypeRef

    /**
     * The list type of the field, e.g. [Map] or [MutableMap].
     */
    val mapType: ClassRef<PositionTypeParameterRef>

    val typeRef: ClassRef<PositionTypeParameterRef>
        get() = mapType.withArgs(keyType, valueType)
}
