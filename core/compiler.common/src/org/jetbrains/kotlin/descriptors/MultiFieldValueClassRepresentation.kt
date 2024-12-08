/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.RigidTypeMarker

class MultiFieldValueClassRepresentation<Type : RigidTypeMarker>(
    override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
) : PreValhallaValueClassRepresentation<Type>() {

    private val map = underlyingPropertyNamesToTypes.toMap()

    override fun containsPropertyWithName(name: Name): Boolean = name in map
    override fun getPropertyTypeByName(name: Name): Type? = map[name]

    override fun toString(): String =
        "MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes=$underlyingPropertyNamesToTypes)"
}