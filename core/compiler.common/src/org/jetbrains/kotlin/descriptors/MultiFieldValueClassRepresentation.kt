/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class MultiFieldValueClassRepresentation<Type : SimpleTypeMarker>(
    override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
) : ValueClassRepresentation<Type>() {

    private val map = underlyingPropertyNamesToTypes.toMap().also {
        require(it.size == underlyingPropertyNamesToTypes.size) { "Some properties have the same names" }
    }

    override fun containsPropertyWithName(name: Name): Boolean = name in map
    override fun getPropertyTypeByName(name: Name): Type? = map[name]

    override fun toString(): String =
        "MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes=$underlyingPropertyNamesToTypes)"
}