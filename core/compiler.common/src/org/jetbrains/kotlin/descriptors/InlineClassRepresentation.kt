/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class InlineClassRepresentation<Type : SimpleTypeMarker>(
    val underlyingPropertyName: Name,
    val underlyingType: Type,
) : ValueClassRepresentation<Type>() {

    override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
        get() = listOf(underlyingPropertyName to underlyingType)

    override fun containsPropertyWithName(name: Name): Boolean = underlyingPropertyName == name

    override fun getPropertyTypeByName(name: Name): Type? = underlyingType.takeIf { containsPropertyWithName(name) }

    override fun toString(): String =
        "InlineClassRepresentation(underlyingPropertyName=$underlyingPropertyName, underlyingType=$underlyingType)"
}

class SealedInlineClassRepresentation<Type : SimpleTypeMarker> : ValueClassRepresentation<Type>() {
    override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
        get() = emptyList()

    override fun containsPropertyWithName(name: Name): Boolean = false

    override fun getPropertyTypeByName(name: Name): Type? = null

    override fun toString(): String = "SealedInlineClassRepresentation"
}