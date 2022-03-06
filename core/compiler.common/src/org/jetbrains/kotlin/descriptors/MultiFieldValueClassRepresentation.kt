/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class MultiFieldValueClassRepresentation<Type : SimpleTypeMarker>(
    val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
) {
    init {
        require(underlyingPropertyNamesToTypes.size > 1) { "MultiFieldValueClassRepresentation has at least 2 properties" }
    }

    private val map = underlyingPropertyNamesToTypes.toMap().also {
        require(it.size == underlyingPropertyNamesToTypes.size) { "Some properties have the same names" }
    }

    fun containsPropertyWithName(name: Name): Boolean = name in map
    fun propertyTypeByName(name: Name): Type? = map[name]

    inline fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): MultiFieldValueClassRepresentation<Other> =
        MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes.map { (key, value) -> key to transform(value) })
}